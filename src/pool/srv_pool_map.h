/**
 * (C) Copyright 2021 Intel Corporation.
 *
 * SPDX-License-Identifier: BSD-2-Clause-Patent
 *
 * GOVERNMENT LICENSE RIGHTS-OPEN SOURCE SOFTWARE
 * The Government's rights to use, modify, reproduce, release, perform, display,
 * or disclose this software are subject to the terms of the BSD-2-Clause-Patent
 * License as provided in Contract No. B609815.
 * Any reproduction of computer software, computer software documentation, or
 * portions thereof marked with this legend must also reproduce the markings.
 */

#ifndef DAOS_SRV_POOL_MAP_H
#define DAOS_SRV_POOL_MAP_H
int
ds_pool_map_tgts_update(struct pool_map *map, struct pool_target_id_list *tgts,
			int opc, bool evict_rank, uint32_t *tgt_map_ver,
			bool print_changes);
#endif /* DAOS_SRV_POOL_MAP_H */
