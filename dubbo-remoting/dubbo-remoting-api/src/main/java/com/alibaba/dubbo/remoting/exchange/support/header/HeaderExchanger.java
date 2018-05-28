/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.alibaba.dubbo.remoting.exchange.support.header;

import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.remoting.Client;
import com.alibaba.dubbo.remoting.RemotingException;
import com.alibaba.dubbo.remoting.Server;
import com.alibaba.dubbo.remoting.Transporters;
import com.alibaba.dubbo.remoting.exchange.ExchangeClient;
import com.alibaba.dubbo.remoting.exchange.ExchangeHandler;
import com.alibaba.dubbo.remoting.exchange.ExchangeServer;
import com.alibaba.dubbo.remoting.exchange.Exchanger;
import com.alibaba.dubbo.remoting.transport.DecodeHandler;

/**
 * DefaultMessenger
 *
 * 基于消息头部( Header )的信息交换者实现类
 */
public class HeaderExchanger implements Exchanger {

    public static final String NAME = "header";

    /**
     * @param url     server url 服务器地址
     * @param handler 数据交换处理器  {@link DubboProtocol#requestHandler}
     * @return
     * @throws RemotingException
     */
    @Override
    public ExchangeClient connect(URL url, ExchangeHandler handler) throws RemotingException {
        // 默认返回 NettyClient
        Client client = Transporters.connect(url, new DecodeHandler(new HeaderExchangeHandler(handler)));
        return new HeaderExchangeClient(client, true);
    }

    /**
     * @param url     server url
     * @param handler 数据交换处理器
     * @return
     * @throws RemotingException
     */
    @Override
    public ExchangeServer bind(URL url, ExchangeHandler handler) throws RemotingException {
        // 默认返回 NettyServer
        Server server = Transporters.bind(url, new DecodeHandler(new HeaderExchangeHandler(handler)));
        return new HeaderExchangeServer(server);
    }

}