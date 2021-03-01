# DFS Overview

DFS即DAOS文件系统，DFS API在DAOS API的顶部直接提供了一个封装的命名空间，该命名空间具有类似于POSIX的API。`名称空间封装在单个DAOS容器下，其中目录和文件是该容器中的对象（还有超级块，后面介绍）`。

封装的名称空间将位于一个DAOS池和一个DAOS容器中。用户提供一个有效的（连接的）池句柄和一个命名空间将位于的开放（open）容器句柄。

## DFS Namespace

创建文件系统时（即，将DAOS容器初始化为封装的名称空间）时，会将保留的对象（具有预定义的对象ID）添加到容器中，并将记录有关名称空间的超块（SB）信息。 SB对象使用对象类OC_RP_XSF复制，并具有保留的OID 0.0。

SB对象包含一个带有magic value的entry，以指明是`POSIX文件系统`。SB对象还将包含文件系统根目录的条目，该条目是一个具有预定义OID（1.0）的保留对象，并使用对象类`OC_RP_XSF`进行复制，并且与目录具有相同的表现形式（详见下一章节）。根ID的OID将作为条目插入到超级块对象中。

SB将如下所示：

~~~~
D-key: "DFS_SB_METADATA"    // 表示文件目录基本信息
A-key: "DFS_MAGIC"
single-value (uint64_t): SB_MAGIC (0xda05df50da05df50)

A-key: "DFS_SB_VERSION"
single-value (uint16_t): Version number of the SB. This is used to determine the layout of the SB (the DKEYs and value sizes).

A-key: "DFS_LAYOUT_VERSION"
single-value (uint16_t): This is used to determine the format of the entries in the DFS namespace (DFS to DAOS mapping).

A-key: "DFS_SB_FEAT_COMPAT"
single-value (uint64_t): flags to indicate feature set like extended attribute support, indexing
// 用于指示功能集，例如扩展属性支持，索引。

A-key: "DFS_SB_FEAT_INCOMPAT"
single-value (uint64_t): flags

A-key: "DFS_SB_MKFS_TIME"
single-value (uint64_t): time when DFS namespace was created

A-key: "DFS_SB_STATE"
single-value (uint64_t): state of FS (clean, corrupted, etc.)

A-key: "DFS_CHUNK_SIZE"
single-value (uint64_t): Default chunk size for files in this container // 容器中文件的默认块大小

A-key: "DFS_OBJ_CLASS"
single-value (uint16_t): Default object class for files in this container // 容器中默认的对象类

D-key: "/"  // 表示文件系统根目录
// rest of akey entries for root are same as in directory entry described below.
~~~~

## DFS Directories

POSIX目录将映射到具有多个dkey的DAOS对象，其中，每个dkey都对应于该目录中的一个条目（比如该目录下的子目录、文件或者符号链接）。dkey值即该目录中的条目名称。另外，dkey将包含一个akey，该akey具有字节数组序列化格式的该条目的所有属性。扩展属性将分别存储在不同键下的单个值中。映射表将如下所示（包含两个扩展属性：xattr1，xattr2）：

~~~~
Directory Object
  D-key "entry1_name"   // 该对象对应的目录下的一个条目
    A-key "DFS_INODE"
      RECX (byte array starting at idx 0):  // 文件属性
        mode_t: permission bit mask + type of entry
        oid: object id of entry
        atime: access time
        mtime: modify time
        ctime: change time
        chunk_size: chunk_size of file (0 if default or not a file)
        syml: symlink value (does not exist if not a symlink)
    A-key "x:xattr1"	// extended attribute name (if any)
    A-key "x:xattr2"	// extended attribute name (if any)
~~~~

扩展属性均以"x:"为前缀。

下面总结了目录testdir与文件、目录和符号链接的映射：

~~~~
testdir$ ls
dir1
file1
syml1 -> dir1

Object testdir
  D-key "dir1"
    A-key "mode" , permission bits + S_IFDIR
    A-key "oid" , object id of dir1
    ...
  D-key "file1"
    A-key "mode" , permission bits + S_IFREG
    A-key "oid" , object id of file1
    ...
  D-key "syml1"
    A-key "mode" , permission bits + S_IFLNK
    A-key "oid" , empty
    A-key "syml", dir1
    ...
~~~~

请注意，通过该映射，inode信息与它对应的条目一起存储在父目录对象中。因此，将**不支持硬链接**，因为不可能创建不同的条目（dkey），该条目实际上指向与当前akeys当前存储在其中的同一组akeys。

## Files

如目录映射所示，文件的条目将插入到其父目录对应的object中，文件条目中的OID将与文件对应。常规文件的对象ID将是DAOS数组对象，而DAOS数组对象本身是具有某些属性（元素大小和块大小）的DAOS对象。在在POSIX文件的情况下，单元大小将始终为1个字节。块大小只能在创建时设置，默认值为1MB。数组对象本身使用整型dkey映射到DAOS对象，其中，每个dkey都包含chunk_size个元素。因此，如果我们有一个文件，文件大小为10个字节，块大小为3个字节，则数组对象将包含以下内容：
~~~~
Object array
  D-key 0
    A-key NULL , array elements [0,1,2]
  D-key 1
    A-key NULL , array elements [3,4,5]
  D-key 2
    A-key NULL , array elements [6,7,8]
  D-key 3
    A-key NULL , array elements [9]
~~~~

有关数组对象布局的更多信息，请参考Array Addons的README.md文件。

通过DAOS Array API可以访问该对象。对文件的所有读取和写入操作都将转换为DAOS数组的读取和写入操作。可以通过DAOS数组set_size/get_size函数设置（截短）或检索文件大小。但是，在这种情况下增加文件大小并不能保证分配了空间。由于DAOS跨不同epochs记录I/O，因此单纯的set_size操作无法支持空间分配。

## Symbolic Links

如目录部分所述，符号链接将没有用于符号链接本身的对象，但在父目录的条目本身中将具有一个包含符号链接实际值的值。

## Access Permissions

所有DFS对象（文件、目录和符号链接）都将继承创建他们的DFS pool的访问权限。因此，当用户尝试访问DFS命名空间中的对象时，会将其真实/有效的uid/gid与连接到池时获得的池uid和gid进行比较。然后使用存储的对对象模式进行检查，并根据请求的访问类型（R、W、X）和对象模式确定访问权限。

在源代码中，这是在函数`check_access()`中实现的

## DFUSE_HL

实现了一个简单的高级保险丝插件（dfuse_hl），以将DFS API和功能与现有POSIX测试和基准（IOR，mdtest等）一起使用。DFS high fuse将一个站点（mounpoint）公开为具有单个池和容器的单个DFS命名空间。