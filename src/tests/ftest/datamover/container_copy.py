#!/usr/bin/python
"""
  (C) Copyright 2020 Intel Corporation.
  Licensed under the Apache License, Version 2.0 (the "License");
  you may not use this file except in compliance with the License.
  You may obtain a copy of the License at
     http://www.apache.org/licenses/LICENSE-2.0
  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS" BASIS,
  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  See the License for the specific language governing permissions and
  limitations under the License.
  GOVERNMENT LICENSE RIGHTS-OPEN SOURCE SOFTWARE
  The Government's rights to use, modify, reproduce, release, perform, display,
  or disclose this software are subject to the terms of the Apache License as
  provided in Contract No. B609815.
  Any reproduction of computer software, computer software documentation, or
  portions thereof marked with this legend must also reproduce the markings.
"""

from data_mover_test_base import DataMoverTestBase
from data_mover_utils import Dcp
from command_utils_base import CommandFailure

# pylint: disable=too-many-ancestors
class ContainerCopy(DataMoverTestBase):
    """Test class Description: Add datamover test to copy large data amongst
                               daos containers and external file system.

    :avocado: recursive
    """

    def test_daoscont1_to_daoscont2(self):
        """Jira ID: DAOS-4782.
        Test Description:
            Copy data from daos cont1 to daos cont2
        Use Cases:
            Create a pool.
            Create POSIX type container.
            Run mdtest -a DFS to create 50K files of size 4K
            Create second container
            Copy data from cont1 to cont2 using dcp
            Run mdtest again, but this time on cont2 and read the files back.
        :avocado: tags=all,full_regression
        :avocado: tags=hw,large
        :avocado: tags=datamover,dcp
        :avocado: tags=containercopy,daoscont1_to_daoscont2
        """

        # test params
        mdtest_flags = self.params.get("mdtest_flags", "/run/mdtest/*")
        file_size = self.params.get("bytes", "/run/mdtest/*")
        # create pool and cont
        self.create_pool()
        self.create_cont(self.pool[0])

        # run mdtest to create data in cont1
        self.mdtest_cmd.write_bytes.update(file_size)
        self.run_mdtest_with_params(
            "DAOS", "/", self.pool[0], self.container[0],
            flags=mdtest_flags[0])

        # create second container
        self.create_cont(self.pool[0])

        # copy data from cont1 to cont2
        self.run_datamover(
            "daoscont1_to_daoscont2 (cont1 to cont2)",
            "DAOS", "/", self.pool[0], self.container[0],
            "DAOS", "/", self.pool[0], self.container[1])

        # run mdtest read on cont2
        self.mdtest_cmd.read_bytes.update(file_size)
        self.run_mdtest_with_params(
            "DAOS", "/", self.pool[0], self.container[1],
            flags=mdtest_flags[1])

    def test_daoscont_to_posixfs(self):
        """Jira ID: DAOS-4782.
        Test Description:
            Copy data from daos container to external Posix file system
        Use Cases:
            Create a pool
            Create POSIX type container.
            Run ior -a DFS on cont1.
            Create cont2
            Copy data from cont1 to cont2 using dcp.
            Copy data from cont2 to external Posix File system.
            (Assuming dfuse mount as external POSIX FS)
            Run ior -a DFS with read verify on copied directory to verify
            data.
        :avocado: tags=all,full_regression
        :avocado: tags=hw,large
        :avocado: tags=datamover,dcp
        :avocado: tags=containercopy,daoscont_to_posixfs
        """

        # create pool and cont
        self.create_pool()
        self.create_cont(self.pool[0])

        # update and run ior on cont1
        self.run_ior_with_params(
            "DAOS", self.ior_cmd.test_file.value,
            self.pool[0], self.container[0])

        # create cont2
        self.create_cont(self.pool[0])

        # copy from daos cont1 to cont2
        self.run_datamover(
            "daoscont_to_posixfs (cont1 to cont2)",
            "DAOS", "/", self.pool[0], self.container[0],
            "DAOS", "/", self.pool[0], self.container[1])

        # create cont3 for dfuse (posix)
        self.create_cont(self.pool[0])

        # start dfuse on cont3
        self.start_dfuse(self.hostlist_clients, self.pool[0], self.container[2])

        # copy from daos cont2 to posix file system (dfuse)
        self.run_datamover(
            "daoscont_to_posixfs (cont2 to posix)",
            "DAOS", "/", self.pool[0], self.container[1],
            "POSIX", self.dfuse.mount_dir.value)

        # update ior params, read back and verify data from posix file system
        dst_path = self.dfuse.mount_dir.value + self.ior_cmd.test_file.value
        self.run_ior_with_params("POSIX", dst_path, flags="-r -R")
