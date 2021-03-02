# DAOS Client Library
DAOS API分为几种功能，以解决DAOS公开的不同功能：
- Management API: pool和target的管理
- Pool Client API: pool访问
- Container API: container的管理、访问以及快照
- Transaction API: 事务模型以及并发控制
- Object, Array and KV APIs: object and data 管理和访问
- Event, Event Queue, and Task API: non-blocking operations
- Addons API: 通过DAOS object API构建的数组和KV操作
- DFS API: DAOS file system API to emulate a POSIX namespace over DAOS
- DUNS API: DAOS统一名称空间API，用于与现有系统名称空间集成。

这些组件中的每个组件都具有关联的README.md文件，这些文件提供了有关它们支持的功能的更多详细信息，但支持非阻塞操作的API除外，此处将对此进行讨论。

The libdaos API is available under [/src/include/daos\_\*](/src/include/) and
associated man pages under [/doc/man/man3/](/doc/man/man3/).

## Event & Event Queue
DAOS API函数可以在阻塞或非阻塞模式下使用。这是通过指向传递给每个API调用的指向DAOS事件的指针来确定的：

- 如果为NULL，则表明该操作将被阻塞。完成操作后，操作将返回。所有失败情况的错误代码都将通过API函数本身的返回代码返回。

- 如果使用了有效事件，则该操作将以非阻塞模式运行，并在内部调度程序中调度了该操作之后，并且在将RPC提交到基础堆栈之后立即返回了该操作。如果调度成功，则操作的返回值是成功，但不表示实际操作成功。返回时可以捕获的错误是无效参数或调度问题。事件完成后，该操作的实际返回码将在事件错误代码（event.ev_error）中可用。、

必须使用单独的API调用创建要使用的有效事件。为了允许用户一次跟踪多个事件，可以将一个事件创建为事件队列的一部分，该事件队列基本上是可以一起进行和轮询的事件的集合。或者，可以在没有事件队列的情况下创建事件，并对其进行单独跟踪。事件完成后，可以将其重新用于另一个DAOS API调用，以最大程度地减少在DAOS库中创建和分配事件的需要。

## Task Engine Integration

DAOS Task API提供了一种以非阻塞方式使用DAOS API的替代方法，同时在DAOS API操作之间构建了任务依赖关系树。这对于使用DAOS的应用程序和中间件库很有用，并且需要构建相互依赖（N-1、1-N，N-N）的DAOS操作时间表。

为了利用tash API，用户将需要创建一个调度程序，可以在其中创建DAOS任务。任务API具有足够的通用性，允许用户混合DAOS特定任务（通过DAOS任务API）和其他用户定义的任务，并在它们之间添加依赖项。

有关在客户端库中如何使用TSE的更多详细信息，请参阅[TSE内部文档]（/ src / common / README.md）了解更多详细信息。