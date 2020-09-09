/**
 * (C) Copyright 2016-2019 Intel Corporation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * GOVERNMENT LICENSE RIGHTS-OPEN SOURCE SOFTWARE
 * The Government's rights to use, modify, reproduce, release, perform, display,
 * or disclose this software are subject to the terms of the Apache License as
 * provided in Contract No. B609815.
 * Any reproduction of computer software, computer software documentation, or
 * portions thereof marked with this legend must also reproduce the markings.
 */
/**
 * This file is for simple tests of addition.
 *
 * tests/suite/daos_addition_simple.c
 *
 *
 */
#define D_LOGFAC	DD_FAC(tests)

#include "daos_iotest.h"
#include <daos/pool.h>
#include <daos/mgmt.h>
#include <daos/container.h>

#define KEY_NR		100
#define OBJ_NR		10
#define OBJ_CLS		OC_RP_3G1
#define OBJ_REPLICAS	3
#define ADDITION_POOL_SIZE	(4ULL << 30)
#define ADDITION_SUBTEST_POOL_SIZE (1ULL << 30)
#define ADDITION_SMALL_POOL_SIZE (1ULL << 28)

static void
addition_dkeys(void **state)
{
	test_arg_t		*arg = *state;
	daos_obj_id_t		oid;
	struct ioreq		req;
	int			i;

	if (!test_runable(arg, 4))
		return;

	oid = dts_oid_gen(OC_RP_3G1, 0, arg->myrank);
	ioreq_init(&req, arg->coh, oid, DAOS_IOD_ARRAY, arg);

	/** Insert 1000 records */
	print_message("Insert %d kv record in object "DF_OID"\n",
		      KEY_NR, DP_OID(oid));
	for (i = 0; i < KEY_NR; i++) {
		char	key[16];

		sprintf(key, "dkey_0_%d", i);
		insert_single(key, "a_key", 0, "data", strlen("data") + 1,
			      DAOS_TX_NONE, &req);
	}
	ioreq_fini(&req);

	D_PRINT("Adding server rank\n");
	addition_single_pool_rank(arg, ranks_to_add, 1);
}

static void
addition_akeys(void **state)
{
	test_arg_t		*arg = *state;
	daos_obj_id_t		oid;
	struct ioreq		req;
	int			i;

	if (!test_runable(arg, 4))
		return;

	oid = dts_oid_gen(OC_RP_3G1, 0, arg->myrank);
	ioreq_init(&req, arg->coh, oid, DAOS_IOD_ARRAY, arg);

	/** Insert 1000 records */
	print_message("Insert %d kv record in object "DF_OID"\n",
		      KEY_NR, DP_OID(oid));
	for (i = 0; i < KEY_NR; i++) {
		char	akey[16];

		sprintf(akey, "%d", i);
		insert_single("dkey_1_0", akey, 0, "data", strlen("data") + 1,
			      DAOS_TX_NONE, &req);
	}
	ioreq_fini(&req);

	addition_single_pool_rank(arg, ranks_to_add, 1);
}

static void
addition_indexes(void **state)
{
	test_arg_t		*arg = *state;
	daos_obj_id_t		oid;
	struct ioreq		req;
	int			i;
	int			j;

	if (!test_runable(arg, 4))
		return;

	oid = dts_oid_gen(OC_RP_3G1, 0, arg->myrank);
	ioreq_init(&req, arg->coh, oid, DAOS_IOD_ARRAY, arg);

	/** Insert 2000 records */
	print_message("Insert %d kv record in object "DF_OID"\n",
		      2000, DP_OID(oid));
	for (i = 0; i < KEY_NR; i++) {
		char	key[16];

		sprintf(key, "dkey_2_%d", i);
		for (j = 0; j < 20; j++)
			insert_single(key, "a_key", j, "data",
				      strlen("data") + 1, DAOS_TX_NONE, &req);
	}
	ioreq_fini(&req);

	addition_single_pool_rank(arg, ranks_to_add, 1);
}

static void
addition_snap_update_recs(void **state)
{
	test_arg_t	*arg = *state;
	daos_obj_id_t	oid;
	struct ioreq	req;
	daos_recx_t	recx;
	char		string[100] = { 0 };
	daos_epoch_t	snap_epoch[5];
	int		i;
	int		rc;

	if (!test_runable(arg, 4))
		return;

	oid = dts_oid_gen(OC_RP_3G1, 0, arg->myrank);
	ioreq_init(&req, arg->coh, oid, DAOS_IOD_ARRAY, arg);
	for (i = 0; i < 5; i++)
		sprintf(string + strlen(string), "old-snap%d", i);

	recx.rx_idx = 0;
	recx.rx_nr = strlen(string);
	insert_recxs("d_key", "a_key", 1, DAOS_TX_NONE, &recx, 1, string,
		     strlen(string) + 1, &req);

	for (i = 0; i < 5; i++) {
		char data[20] = { 0 };

		/* Update string for each snapshot */
		daos_cont_create_snap(arg->coh, &snap_epoch[i], NULL, NULL);
		sprintf(data, "new-snap%d", i);
		recx.rx_idx = i * strlen(data);
		recx.rx_nr = strlen(data);
		insert_recxs("d_key", "a_key", 1, DAOS_TX_NONE, &recx, 1, data,
			     strlen(data) + 1, &req);
	}

	addition_single_pool_rank(arg, ranks_to_add, 1);

	for (i = 0; i < 5; i++) {
		rc = daos_obj_verify(arg->coh, oid, snap_epoch[i]);
		assert_int_equal(rc, 0);
	}

	rc = daos_obj_verify(arg->coh, oid, DAOS_EPOCH_MAX);
	assert_int_equal(rc, 0);

	ioreq_fini(&req);
}

static void
addition_snap_punch_recs(void **state)
{
	test_arg_t	*arg = *state;
	daos_obj_id_t	oid;
	struct ioreq	req;
	daos_recx_t	recx;
	char		string[100];
	daos_epoch_t	snap_epoch[5];
	int		i;
	int		rc;

	if (!test_runable(arg, 4))
		return;

	oid = dts_oid_gen(OC_RP_3G1, 0, arg->myrank);
	ioreq_init(&req, arg->coh, oid, DAOS_IOD_ARRAY, arg);
	for (i = 0; i < 5; i++)
		sprintf(string + strlen(string), "old-snap%d", i);

	recx.rx_idx = 0;
	recx.rx_nr = strlen(string);
	insert_recxs("d_key", "a_key", 1, DAOS_TX_NONE, &recx, 1, string,
		     strlen(string) + 1, &req);

	for (i = 0; i < 5; i++) {
		/* punch string */
		daos_cont_create_snap(arg->coh, &snap_epoch[i], NULL, NULL);
		recx.rx_idx = i * 9; /* strlen("old-snap%d") */
		recx.rx_nr = 9;
		punch_recxs("d_key", "a_key", &recx, 1, DAOS_TX_NONE, &req);
	}

	addition_single_pool_rank(arg, ranks_to_add, 1);

	for (i = 0; i < 5; i++) {
		rc = daos_obj_verify(arg->coh, oid, snap_epoch[i]);
		assert_int_equal(rc, 0);
	}

	rc = daos_obj_verify(arg->coh, oid, DAOS_EPOCH_MAX);
	assert_int_equal(rc, 0);

	ioreq_fini(&req);
}

static void
addition_snap_update_keys(void **state)
{
	test_arg_t	*arg = *state;
	daos_obj_id_t	oid;
	struct ioreq	req;
	daos_epoch_t	snap_epoch[5];
	int		i;

	if (!test_runable(arg, 4))
		return;

	oid = dts_oid_gen(OC_RP_3G1, 0, arg->myrank);
	ioreq_init(&req, arg->coh, oid, DAOS_IOD_ARRAY, arg);
	/* Insert dkey/akey by different snapshot */
	for (i = 0; i < 5; i++) {
		char dkey[20] = { 0 };
		char akey[20] = { 0 };

		/* Update string for each snapshot */
		daos_cont_create_snap(arg->coh, &snap_epoch[i], NULL, NULL);
		sprintf(dkey, "dkey_%d", i);
		sprintf(akey, "akey_%d", i);
		insert_single(dkey, "a_key", 0, "data", 1, DAOS_TX_NONE, &req);
		insert_single("dkey", akey, 0, "data", 1, DAOS_TX_NONE, &req);
	}

	addition_single_pool_rank(arg, ranks_to_add, 1);

	daos_fail_loc_set(DAOS_OBJ_SPECIAL_SHARD);
	for (i = 0; i < OBJ_REPLICAS; i++) {
		uint32_t	number;
		daos_key_desc_t kds[10];
		daos_anchor_t	anchor = { 0 };
		char		buf[256];
		int		buf_len = 256;
		int		j;

		daos_fail_value_set(i);
		for (j = 0; j < 5; j++) {
			daos_handle_t	th_open;

			memset(&anchor, 0, sizeof(anchor));
			daos_tx_open_snap(arg->coh, snap_epoch[j], &th_open,
					  NULL);
			number = 10;
			enumerate_dkey(th_open, &number, kds, &anchor, buf,
				       buf_len, &req);

			assert_int_equal(number, j > 0 ? j + 1 : 0);

			number = 10;
			memset(&anchor, 0, sizeof(anchor));
			enumerate_akey(th_open, "dkey", &number, kds, &anchor,
				       buf, buf_len, &req);

			assert_int_equal(number, j);
			daos_tx_close(th_open, NULL);
		}
		number = 10;
		memset(&anchor, 0, sizeof(anchor));
		enumerate_dkey(DAOS_TX_NONE, &number, kds, &anchor, buf,
			       buf_len, &req);
		assert_int_equal(number, 6);

		number = 10;
		memset(&anchor, 0, sizeof(anchor));
		enumerate_akey(DAOS_TX_NONE, "dkey", &number, kds, &anchor,
			       buf, buf_len, &req);
		assert_int_equal(number, 5);
	}

	ioreq_fini(&req);
}

static void
addition_snap_punch_keys(void **state)
{
	test_arg_t	*arg = *state;
	daos_obj_id_t	oid;
	struct ioreq	req;
	daos_epoch_t	snap_epoch[5];
	int		i;

	if (!test_runable(arg, 4))
		return;

	oid = dts_oid_gen(OC_RP_3G1, 0, arg->myrank);
	ioreq_init(&req, arg->coh, oid, DAOS_IOD_ARRAY, arg);
	/* Insert dkey/akey */
	for (i = 0; i < 5; i++) {
		char dkey[20] = { 0 };
		char akey[20] = { 0 };

		/* Update string for each snapshot */
		sprintf(dkey, "dkey_%d", i);
		sprintf(akey, "akey_%d", i);
		insert_single(dkey, "a_key", 0, "data", 1, DAOS_TX_NONE, &req);
		insert_single("dkey", akey, 0, "data", 1, DAOS_TX_NONE, &req);
	}

	/* Insert dkey/akey by different epoch */
	for (i = 0; i < 5; i++) {
		char dkey[20] = { 0 };
		char akey[20] = { 0 };

		daos_cont_create_snap(arg->coh, &snap_epoch[i], NULL, NULL);

		sprintf(dkey, "dkey_%d", i);
		sprintf(akey, "akey_%d", i);
		punch_dkey(dkey, DAOS_TX_NONE, &req);
		punch_akey("dkey", akey, DAOS_TX_NONE, &req);
	}

	addition_single_pool_rank(arg, ranks_to_add, 1);

	daos_fail_loc_set(DAOS_OBJ_SPECIAL_SHARD);
	for (i = 0; i < OBJ_REPLICAS; i++) {
		daos_key_desc_t  kds[10];
		daos_anchor_t	 anchor;
		char		 buf[256];
		int		 buf_len = 256;
		uint32_t	 number;
		int		 j;

		daos_fail_value_set(i);
		for (j = 0; j < 5; j++) {
			daos_handle_t th_open;

			daos_tx_open_snap(arg->coh, snap_epoch[j], &th_open,
					  NULL);
			number = 10;
			memset(&anchor, 0, sizeof(anchor));
			enumerate_dkey(th_open, &number, kds, &anchor, buf,
				       buf_len, &req);
			assert_int_equal(number, 6 - j);

			number = 10;
			memset(&anchor, 0, sizeof(anchor));
			enumerate_akey(th_open, "dkey", &number, kds,
				       &anchor, buf, buf_len, &req);
			assert_int_equal(number, 5 - j);

			daos_tx_close(th_open, NULL);
		}

		number = 10;
		memset(&anchor, 0, sizeof(anchor));
		enumerate_dkey(DAOS_TX_NONE, &number, kds, &anchor, buf,
			       buf_len, &req);
		assert_int_equal(number, 1);

		number = 10;
		memset(&anchor, 0, sizeof(anchor));
		enumerate_akey(DAOS_TX_NONE, "dkey", &number, kds, &anchor,
			       buf, buf_len, &req);
		assert_int_equal(number, 0);
	}

	ioreq_fini(&req);
}

static void
addition_multiple(void **state)
{
	test_arg_t	*arg = *state;
	daos_obj_id_t	oid;
	struct ioreq	req;
	int		i;
	int		j;
	int		k;

	if (!test_runable(arg, 4))
		return;

	oid = dts_oid_gen(OC_RP_3G1, 0, arg->myrank);
	ioreq_init(&req, arg->coh, oid, DAOS_IOD_ARRAY, arg);

	/** Insert 1000 records */
	print_message("Insert %d kv record in object "DF_OID"\n",
		      1000, DP_OID(oid));
	for (i = 0; i < 10; i++) {
		char	dkey[16];

		sprintf(dkey, "dkey_3_%d", i);
		for (j = 0; j < 10; j++) {
			char	akey[16];

			sprintf(akey, "akey_%d", j);
			for (k = 0; k < 10; k++)
				insert_single(dkey, akey, k, "data",
					      strlen("data") + 1,
					      DAOS_TX_NONE, &req);
		}
	}
	ioreq_fini(&req);

	addition_single_pool_rank(arg, ranks_to_add, 1);
}

static void
addition_large_rec(void **state)
{
	test_arg_t		*arg = *state;
	daos_obj_id_t		oid;
	struct ioreq		req;
	int			i;
	char			buffer[5000];

	if (!test_runable(arg, 4))
		return;

	oid = dts_oid_gen(OC_RP_3G1, 0, arg->myrank);
	ioreq_init(&req, arg->coh, oid, DAOS_IOD_ARRAY, arg);

	/** Insert 1000 records */
	print_message("Insert %d kv record in object "DF_OID"\n",
		      KEY_NR, DP_OID(oid));
	memset(buffer, 'a', 5000);
	for (i = 0; i < KEY_NR; i++) {
		char	key[16];

		sprintf(key, "dkey_4_%d", i);
		insert_single(key, "a_key", 0, buffer, 5000, DAOS_TX_NONE,
			      &req);
	}
	ioreq_fini(&req);

	addition_single_pool_rank(arg, ranks_to_add, 1);
}

/** create a new pool/container for each test */
static const struct CMUnitTest addition_tests[] = {
	{"ADDITION1: addition small rec multiple dkeys",
	 addition_dkeys, addition_small_sub_setup, test_teardown},
	{"ADDITION2: addition small rec multiple akeys",
	 addition_akeys, addition_small_sub_setup, test_teardown},
	{"addition3: addition small rec multiple indexes",
	 addition_indexes, addition_small_sub_setup, test_teardown},
	{"ADDITION4: addition small rec multiple keys/indexes",
	 addition_multiple, addition_small_sub_setup, test_teardown},
	{"ADDITION5: addition large rec single index",
	 addition_large_rec, addition_small_sub_setup, test_teardown},
	{"ADDITION6: addition records with multiple snapshots",
	 addition_snap_update_recs, addition_small_sub_setup, test_teardown},
	{"ADDITION7: addition punch/records with multiple snapshots",
	 addition_snap_punch_recs, addition_small_sub_setup, test_teardown},
	{"ADDITION8: addition keys with multiple snapshots",
	 addition_snap_update_keys, addition_small_sub_setup, test_teardown},
	{"ADDITION9: addition keys/punch with multiple snapshots",
	 addition_snap_punch_keys, addition_small_sub_setup, test_teardown},
};

int
run_daos_addition_simple_test(int rank, int size, int *sub_tests,
			      int sub_tests_size)
{
	int rc = 0;

	MPI_Barrier(MPI_COMM_WORLD);
	if (sub_tests_size == 0) {
		sub_tests_size = ARRAY_SIZE(addition_tests);
		sub_tests = NULL;
	}

	run_daos_sub_tests_only("DAOS addition simple tests", addition_tests,
				ARRAY_SIZE(addition_tests), sub_tests,
				sub_tests_size);

	MPI_Barrier(MPI_COMM_WORLD);

	return rc;
}