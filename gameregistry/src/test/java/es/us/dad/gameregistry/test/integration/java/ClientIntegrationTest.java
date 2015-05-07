package es.us.dad.gameregistry.test.integration.java;

import es.us.dad.gameregistry.client.GameRegistryClient;
import es.us.dad.gameregistry.client.GameRegistryResponse;
import es.us.dad.gameregistry.client.GameRegistryResponse.ResponseType;
import es.us.dad.gameregistry.server.domain.GameSession;

import org.junit.Test;
import org.vertx.java.core.AsyncResult;
import org.vertx.java.core.AsyncResultHandler;
import org.vertx.java.core.Handler;
import org.vertx.testtools.TestVerticle;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Date;

import static org.vertx.testtools.VertxAssert.*;

/**
 * Example Java integration test that deploys the module that this project builds.
 *
 * Quite often in integration tests you want to deploy the same module for all tests and you don't want tests
 * to start before the module has been deployed.
 *
 * This test demonstrates how to do that.
 */
public class ClientIntegrationTest extends TestVerticle {

    @Test
    public void testClientCreateSession() throws UnknownHostException {
        GameRegistryClient client = new GameRegistryClient(InetAddress.getLocalHost(), vertx);
        client.setUser("test");
        client.setToken("test");
        
        final GameSession session = new GameSession();
        session.setStart(new Date());
        session.setEnd(new Date(session.getStart().getTime() + 1000*60*10)); // Ten minutes after start
        session.setGame("testGame");
        session.setUser("testUser");
        
        client.addSession(session, new Handler<GameRegistryResponse>() {

            @Override
            public void handle(GameRegistryResponse event) {
            	container.logger().info("Server's response: " + event.responseType.toString());
            	container.logger().info("Http response: " + event.innerHttpResponse.statusCode() + " " + event.innerHttpResponse.statusMessage());
            	assertTrue(event.responseType == ResponseType.OK);
            	assertTrue(event.sessions.length == 1);
            	assertTrue(event.sessions[0].getGame() == "testGame");
            	assertTrue(event.sessions[0].getUser() == "testUser");
            	
                testComplete();
            }
        });
    }
    
    /*@Test
    public void testClientGetSession() throws UnknownHostException {
    	GameRegistryClient client = new GameRegistryClient(InetAddress.getLocalHost(), vertx)
    		.setUser("test")
    		.setToken("test");
    	
    }*/

    @Override
    public void start() {
        // Make sure we call initialize() - this sets up the assert stuff so assert functionality works correctly
        initialize();
        // Deploy the module - the System property `vertx.modulename` will contain the name of the module so you
        // don't have to hardecode it in your tests
        container.deployModule(System.getProperty("vertx.modulename"), new AsyncResultHandler<String>() {
            @Override
            public void handle(AsyncResult<String> asyncResult) {
                // Deployment is asynchronous and this this handler will be called when it's complete (or failed)
                if (asyncResult.failed()) {
                    container.logger().error("Failed to deploy " + System.getProperty("vertx.modulename") + ": " + asyncResult.cause());
                }
                assertTrue(asyncResult.succeeded());
                assertNotNull("deploymentID should not be null", asyncResult.result());
                // If deployed correctly then start the tests!
                startTests();
            }
        });
    }

}