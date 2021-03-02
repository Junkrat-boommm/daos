# DAOS Arrays

DAOS数组是一个特殊的DAOS对象，用于向用户公开逻辑一维数组。用户创建的数组具有不变的记录大小和块大小。提供了其他API来访问数组（read，write，punch）。

## Array Representation

DAOS KV API上的数组表示形式是用整数类型的DKey完成的，其中每个DKey都保存chunk_size记录。每个DKey都有1个AKey，它们的NULL值在数组类型范围内保存用户数组数据。第一个DKey（为0）不保存任何用户数据，而仅保存数组元数据：

~~~~~~
DKey: 0
Single Value: 3 uint64_t
       [0] = magic value (0xdaca55a9daca55a9)
       [1] = array cell size
       [2] = array chunk size
~~~~~~

为了说明数组映射，假设我们有一个由10个元素组成的逻辑数组，块大小为3。DAOSKV表示为：

~~~~~~
DKey: 1
Array records: 0, 1, 2
DKey: 2
Array records: 3, 4, 5
DKey: 3
Array records: 6, 7, 8
DKey: 4
Array records: 9
~~~~~~

## API and Implementation

API（include/daos_array.h）提供一下操作：
- 使用指定的数组的不变元数据创建一个数组。
- 打开一个现有数组，该数组返回与该数组关联的元数据。
- 从数组对象读取
- 写入到数组对象
- 设置数组的大小（截断）。请注意，这不等同于预分配。
- 获取数组对象的长度
- 从数组中打出一系列记录。
- 删除数组

阵列API是使用DAOS Task API实现的。例如，读和写操作为每个DKey创建一个I/O操作，并将它们插入到具有父任务的任务引擎中，该父任务取决于执行I/O的所有子任务。