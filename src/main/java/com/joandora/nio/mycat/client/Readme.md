# 模拟mycat nio架构
> NIOAcceptor 接受客户端的连接
> NIOReactor  负责调度socketChannel
> NIOHandler  负责读写数据
