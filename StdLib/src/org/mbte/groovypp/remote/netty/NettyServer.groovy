package org.mbte.groovypp.remote.netty

import org.jboss.netty.channel.socket.nio.NioServerSocketChannelFactory
import org.jboss.netty.bootstrap.ServerBootstrap
import java.util.concurrent.Executors
import org.jboss.netty.handler.codec.serialization.ObjectEncoder
import org.jboss.netty.handler.codec.serialization.ObjectDecoder
import org.jboss.netty.channel.Channel
import groovy.remote.ClusterNode
import groovy.util.concurrent.SupervisedChannel

@Typed class NettyServer extends SupervisedChannel {
    int connectionPort

    InetAddress multicastGroup
    int         multicastPort

    ClusterNode clusterNode
    NioServerSocketChannelFactory serverFactory

    private Channel serverChannel

    protected void doStartup() {
        super.doStartup()

        serverFactory = [Executors.newCachedThreadPool(),Executors.newCachedThreadPool()]

        ServerBootstrap bootstrap = [serverFactory]

        bootstrap.setOption("child.tcpNoDelay", true)
        bootstrap.setOption("child.keepAlive", true)

        SimpleChannelHandlerEx handler = [
                createConnection: {ctx ->
                    new NettyConnection(channel: ctx.channel, clusterNode:clusterNode)
                }
        ]

        bootstrap.pipeline.addLast("object.encoder", new ObjectEncoder())
        bootstrap.pipeline.addLast("object.decoder", new ObjectDecoder())
        bootstrap.pipeline.addLast("handler", handler)

        // Bind and startup to accept incoming connections.
        serverChannel = bootstrap.bind(new InetSocketAddress(InetAddress.getLocalHost(), connectionPort))

        if (multicastGroup && multicastPort)
            startupChild (new BroadcastThread.Sender([
                    multicastGroup: multicastGroup,
                    multicastPort:  multicastPort,
                    dataToTransmit: InetDiscoveryInfo.toBytes(clusterNode.id, (InetSocketAddress)serverChannel.getLocalAddress())
            ]))
    }

    protected void doShutdown () {
        serverChannel?.close()
        serverFactory.releaseExternalResources()
        serverFactory = null
    }
}
