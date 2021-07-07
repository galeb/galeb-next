package io.galeb.router.tests.handlers;

import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.context.ApplicationContext;

import io.galeb.core.entity.Environment;
import io.galeb.core.entity.Project;
import io.galeb.core.entity.VirtualHost;
import io.galeb.router.configurations.ManagerClientCacheConfiguration.ManagerClientCache;
import io.galeb.router.handlers.NameVirtualHostDefaultHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.ServerConnection;
import io.undertow.server.handlers.NameVirtualHostHandler;
import io.undertow.util.Headers;
import io.undertow.util.Methods;

public class NameVirtualHostDefaultHandlerTest {
	
	@Test
	public void testHandleRequestWithTwoConsecutivesRequestShouldNotReturnVirtualhostNotFound() throws Exception {
		VirtualHost virtualHost = new VirtualHost("virtualhost-name", new Environment("env-test"), new Project("project-name"));
		
		ServerConnection connection = Mockito.mock(ServerConnection.class);
		HttpServerExchange exchange = new HttpServerExchange(connection);
		exchange.getRequestHeaders().add(Headers.HOST, "virtualhost-name");
		exchange.setRequestMethod(Methods.GET);
		exchange.setRequestURI("http://localhost:8080/");
		
		ApplicationContext context = Mockito.mock(ApplicationContext.class);
		
		NameVirtualHostHandler nameVirtualHostHandler = Mockito.spy(new NameVirtualHostHandler());
		Mockito.doNothing().when(nameVirtualHostHandler).handleRequest(exchange);
		
		Mockito.doReturn(nameVirtualHostHandler).when(context).getBean(NameVirtualHostHandler.class);
		
		ManagerClientCache cache = new ManagerClientCache();
		cache.put("virtualhost-name", virtualHost);
		
		NameVirtualHostDefaultHandler nameVirtualHostDefaultHandler = Mockito.spy(new NameVirtualHostDefaultHandler(context, cache));
		
		nameVirtualHostDefaultHandler.handleRequest(exchange);
		nameVirtualHostDefaultHandler.handleRequest(exchange);
		
		Mockito.verify(nameVirtualHostDefaultHandler, Mockito.never()).handleVirtualhostNotFound(exchange);;
	}
}
