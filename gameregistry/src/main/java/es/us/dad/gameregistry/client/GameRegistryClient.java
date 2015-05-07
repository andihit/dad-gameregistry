package es.us.dad.gameregistry.client;

import es.us.dad.gameregistry.GameRegistryConstants;
import es.us.dad.gameregistry.client.GameRegistryResponse.ResponseType;
import es.us.dad.gameregistry.server.domain.GameSession;

import org.vertx.java.core.Handler;
import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.http.HttpClient;
import org.vertx.java.core.http.HttpClientRequest;
import org.vertx.java.core.http.HttpClientResponse;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.Vertx;

import java.net.InetAddress;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.util.UUID;

import com.hazelcast.util.AddressUtil.InvalidAddressException;

/**
 * Helper class to execute requests on a GameRegistry server.
 * 
 * This class is coded with a fluent interface in mind, as is the HttpClient from vertx. It is
 * also coded to be used asynchronously.
 * 
 * Intended to be used like this:
 * <pre><code>
 *  // In verticle's Start() method probably:
 * 	// Create the client 
 *  // In this example the GameRegistry server ip is 1.2.3.4 and it is listening in the default port.
 *  InetAddress addr = InetAddress.getByName("1.2.3.4");
 * 	GameRegistryClient client = new GameRegistryClient(addr, vertx)
 *                                  .setUser(userId)
 *                                  .setToken(token);
 *  ...
 *  
 *  // Somewhere in your code, an example method that retrieves a session
 *  // and 'doSomethingWith' it will look like this:
 *  void requestSomeSession(UUID sessionId) {
 *    // Do a request
 *    client.getSession(id, new Handler&lt;GameRegistryResponse&gt;() {
 *  	&#64;Override
 *  	public void handle(GameRegistryResponse response) {
 *        if (response.responseType == ResponseType.OK) {
 *          // Request was successful
 *          doSomethingWith(response.sessions[0]);  
 *        }
 *        else {
 *          // Error happened. Check response.responseType value and handle the error
 *          handleError(response);
 *        }
 *  	}
 *    });
 *  }
 * </code></pre> 
 * 
 * Every request made to the GameRegistry server should contain an User-Token pair that
 * will be validated by the server through a LoginServer. Set them up using setUser and 
 * setToken methods before doing any request or an IllegalArgumentException will be raised.
 * 
 * @see GameRegistryResponse
 */
public class GameRegistryClient {
	/**
	 * Default client port.
	 */
	public static final int DEFAULT_PORT = 8080;
	
	private HttpClient httpClient = null;
	private InetAddress host = null;
	private int port = -1;
	private String user = "";
	private String token = "";
	
	/**
	 * Builds a new GameRegistryClient.
	 * 
	 * The port will be the default GameRegistry port, which is 8080.
	 * @param host Address where the GameRegistry server is hosted.
	 * @param vertx The Vertx instance, used to create an HttpClient.
	 * @throws InvalidAddressException
	 */
	public GameRegistryClient(InetAddress host, Vertx vertx) throws InvalidAddressException {
		initialize(host, DEFAULT_PORT, vertx.createHttpClient());
	}
	
	/**
	 * Builds a new GameRegistryClient.
	 * 
	 * @param host Address where the GameRegistry server is hosted.
	 * @param port Port number where the server is listening to.
	 * @param vertx The vertx instance, used to create an HttpClient.
	 * @throws InvalidAddressException
	 */
	public GameRegistryClient(InetAddress host, int port, Vertx vertx) throws InvalidAddressException {
		initialize(host, port, vertx.createHttpClient());
	}
	
	/**
	 * Builds a new GameRegistryClient.
	 * 
	 * This constructor expects the host address as a string of the form "&lt;host&gt;:&lt;port&gt;",
	 * where host is the textual representation of the ip address (ie "1.2.3.4") or a host name
	 * like "gameregistry.cloudall.net" and port is a number between 0 and 65535 (by default, 
	 * GameRegistry servers listen on 8080).
	 * 
	 * Warning: this constructor does a synchronous DNS resolve if the address parameter
	 * contains an unresolved host name. Use an IP to avoid it, or use 
	 * org.vertx.java.core.dns.DnsClient to do a DNS resolve of name asynchronously.
	 * 
	 * @param address Textual representation of the host address.
	 * @param vertx Vertx instance, used to create an HttpClient.
	 * @throws URISyntaxException Invalid port
	 * @throws InvalidAddressException Something is wrong with the address
	 * @throws UnknownHostException Unable to resolve the host name
	 */
	public GameRegistryClient(String address, Vertx vertx) throws URISyntaxException, InvalidAddressException, UnknownHostException {
		int port;
		InetAddress ip;
		
		int colonsIndex = address.indexOf(':');
		if (colonsIndex == -1) {
			port = DEFAULT_PORT;
			ip = InetAddress.getByName(address);
		}
		else {
			try {
				port = Integer.parseInt(address.substring(colonsIndex + 1));
				// This might do a synchronous DNS resolve if contains an unresolved host name
				ip = InetAddress.getByName(address.substring(0, colonsIndex - 1));
			} 
			catch(NumberFormatException e) { 
				throw new URISyntaxException(address, "Invalid port number.");
			}
			catch(Exception e) {
				throw new InvalidAddressException(e.toString());
			}
		}
		
		initialize(ip, port, httpClient);
	}
	
	private void initialize(InetAddress host, int port, HttpClient httpClient) throws InvalidAddressException {
		if (port < 0 || port > 65535) {
			throw new InvalidAddressException("Invalid port: " + port);
		}
		
		this.port = port;
		this.host = host;
		this.httpClient = httpClient.setPort(port);
	}
	
	/**
	 * Sets the user identifier to be used in the request.
	 * @param user identifier of the user.
	 * @return This client (fluent interface).
	 */
	public GameRegistryClient setUser(String user) {
		this.user = user;
		return this;
	}
	
	/**
	 * Sets the token to be used in the request.
	 * @param token String containing the token
	 * @return This client (fluent interface).
	 */
	public GameRegistryClient setToken(String token) {
		this.token = token;
		return this;
	}
	
	/* 
	 * Next should be methods to perform requests on the server. Needs more work, like
	 * 
	 * getUserLastSession (user, ...)
	 * Get last session of a given user.
	 * 
	 * getUserSessions (user, begin = 0, count = 20, ...)
	 * Get the first 'count' sessions after session number 'begin' of 'user'.
	 * 
	 * getUserSession (user, sessionid, ...)
	 * Get a specific session given a session id
	 * 
	 * Probably all will be helper methods that will do a 'GET /sessions' with some
	 * filter parameters under the hood.
	 * ?
	 */
	
	/**
	 * Creates an HttpClientRequest object and sets it up for the GameRegistryServer.
	 *  
	 * @param path Route of the resource. For example "/sessions/1".
	 * @param method Http method to use in the request (GET/POST/PUT/DELETE).
	 * @param gameRegistryResponseHandler The handler in charge of the response.
	 * @return The newly created request.
	 */
	private HttpClientRequest createHttpRequest(String path, String method, Handler<GameRegistryResponse> gameRegistryResponseHandler) {
		if (!method.equals("GET") && !method.equals("POST") && !method.equals("PUT") && !method.equals("DELETE"))
			throw new IllegalArgumentException("Unsuported method: " + method);
		
		HttpClientHandlers handlers = new HttpClientHandlers(gameRegistryResponseHandler);
		HttpClientRequest req = httpClient.request(method, path, handlers.httpHandler());
		req.exceptionHandler(handlers.exceptionHandler());
		this.addUserTokenToRequest(this.user, this.token, req);
		
		return req;
	}
	
	// GET /sessions
	/**
	 * Requests a collection of GameSessions from the GameRegistry server.
	 * 
	 * @param filterParams Filtering options.
	 * @param responseHandler The handler that will process the response.
	 * @return This client (fluent interface).
	 */
	public GameRegistryClient getSessions(Object filterParams, Handler<GameRegistryResponse> responseHandler) {
		// TODO filterParams needs the protocol to be defined to be more specific
		String url = hostString() + "/sessions";
		HttpClientRequest req = createHttpRequest(url, "GET", responseHandler);
		// TODO Add filtering parameters
		
		req.end();
		
		return this;
	}
	
	// POST /sessions
	/**
	 * Adds a new session to the GameRegistry server.
	 * 
	 * The session to be added will get a new identifier when added to the server.
	 * The previous session id will be ignored.
	 * 
	 * The response will contain the new game session with its new UUID.
	 * 
	 * Intended use:
	 * <pre><code>
	 *  GameRegistryClient client = new GameRegistryClient("1.2.3.4", "8080");
	 *  
	 *  GameSession newSession = new GameSession();
	 *  newSession.game = "MyFunnyGame";
	 *  newSession.start = new Date(...);
	 *  newSession.end = new Date(...);
	 *  newSession.user = userId;
	 *  
	 *  client.addSession(session, new Handler&lt;GameRegistryResponse&gt;() {
	 *    @Override
	 *    void handle(GameRegistryResponse response) {
	 *      // Add here code to handle the response (check response.responseType, etc)
	 *    }
	 *  });
	 * </code></pre>
	 * 
	 * @param session Session to add.
	 * @param responseHandler A handler for the server's response.
	 * @return This client (fluent interface).
	 */
	public GameRegistryClient addSession(GameSession session, Handler<GameRegistryResponse> responseHandler) {
		String url = "/sessions";
		HttpClientRequest req = createHttpRequest(url, "POST", responseHandler);
		req.headers().set("Content-Type", "application/json");

		// The JsonObject sent to the server should not contain an id, it will get
		// a new id when added to the collection.
		JsonObject jsonSession = new JsonObject(session.toJsonMap());
		jsonSession.removeField("id");
		req.end(jsonSession.encodePrettily());
		
		return this;
	}
	
	// GET /session/:sessionID
	/**
	 * Requests a single GameSession to the GameRegistry server.
	 * @param sessionId Identifier of the GameSession object to retrieve.
	 * @param responseHandler A handler for the server's response.
	 * @return This client.
	 */
	public GameRegistryClient getSession(UUID sessionId, Handler<GameRegistryResponse> responseHandler) {
		String url = hostString() + "/session/" + sessionId;
		HttpClientRequest req = createHttpRequest(url, "GET", responseHandler);
		req.end();
		
		return this;
	}
	
	// PUT /session/:session.id
	/**
	 * Replaces a GameSession in the GameRegistryServer with a new one.
	 * 
	 * The GameRegistry server will find a session with an identifier equal to the
	 * session provided to this method and replace that session with the new one.
	 * 
	 * @param session The new version of the GameSession to be replaced. 
	 * @param responseHandler
	 * @return This client.
	 */
	public GameRegistryClient updateSession(GameSession session, Handler<GameRegistryResponse> responseHandler) {
		String url = hostString() + "/session/" + session.getId();
		HttpClientRequest req = createHttpRequest(url, "PUT", responseHandler);
		req.headers().set("Content-Type", "application/json");
		req.end(new JsonObject(session.toJsonMap()).toString());
		
		return this;
	}
	
	// DELETE /session/:session.id
	/**
	 * Removes a GameSession from the GameRegistryServer.
	 * @param sessionId Identifier of the session to be removed.
	 * @param responseHandler Handler of the GameRegistry server's response.
	 * @return This client.
	 */
	public GameRegistryClient deleteSession(UUID sessionId, Handler<GameRegistryResponse> responseHandler) {
		String url = hostString() + "/session/" + sessionId;
		HttpClientRequest req = createHttpRequest(url, "DELETE", responseHandler);
		req.end();
		
		return this;
	}
	
	private String hostString() {
		return this.host.getHostAddress() + ":" + this.port;
	}
	
	private void addUserTokenToRequest(String user, String token, HttpClientRequest request) throws IllegalArgumentException {
		if (user == null || token == null || user.isEmpty() || token.isEmpty())
			throw new IllegalArgumentException("At least one of the parameters is null or empty.");
		else {
			request.putHeader(GameRegistryConstants.GAMEREGISTRY_USER_HEADER, user)
				   .putHeader(GameRegistryConstants.GAMEREGISTRY_TOKEN_HEADER, token);
		}
	}
	
	/**
	 * Provides handlers for a GameRegistry Http request.
	 * 
	 * This class is only used internally in the GameRegistryClient to encapsulate repetitive
	 * code related to Http and exception handlers in the HttpClientRequest involved in the
	 * GameRegistry request.
	 */
	private class HttpClientHandlers {
		private Handler<GameRegistryResponse> gameRegistryResponseHandler;
		
		HttpClientHandlers(Handler<GameRegistryResponse> gameRegistryHandler) {
			this.gameRegistryResponseHandler = gameRegistryHandler;
		}
		
		/**
		 * Returns an HttpClientResponse handler that will parse the http response into a 
		 * GameRegistryResponse and then call the GameRegistryResponse handler from this class.
		 * @return An HttpClientResponse handler.
		 */
		public Handler<HttpClientResponse> httpHandler() {
			return new ResponseHandler(this);
		}
		
		/**
		 * Returns a Throwable handler that will create an according GameRegistryResponse
		 * object signaling an error. Then it will call the GameRegistryResponse handler
		 * from this class.
		 * @return A Throwable handler.
		 */
		public Handler<java.lang.Throwable> exceptionHandler() {
			return new ExceptionHandler(this);
		}
		
		/**
		 * An HttpClientResponse handler that parses the response into a GameRegistryResponse
		 * and call a handler for that. 
		 * @see GameRegistryClient.HttpClientHandlers.httpHandler
		 */
		private class ResponseHandler implements Handler<HttpClientResponse> {
			private HttpClientHandlers handlers;
			
			ResponseHandler(HttpClientHandlers handlers) {
				this.handlers = handlers;
			}
			
			@Override
			public void handle(final HttpClientResponse httpResponse) {
				// We need to parse the response when all the response body is received
				httpResponse.bodyHandler(new Handler<Buffer>() {
					@Override
					public void handle(Buffer body) {
						GameRegistryResponse response = GameRegistryResponse.fromHttpResponse(httpResponse, body);
						handlers.gameRegistryResponseHandler.handle(response);
					}
				});
			}
		}
		
		/**
		 * A Throwable handler that creates a GameRegistryResponse object signaling an
		 * error and calls a handler for it.
		 * @see GameRegistryClient.HttpClientHandlers.exceptionHandler
		 */
		private class ExceptionHandler implements Handler<java.lang.Throwable> {
			private HttpClientHandlers handlers;
			
			ExceptionHandler(HttpClientHandlers handlers) {
				this.handlers = handlers;
			}
			
			@Override
			public void handle(java.lang.Throwable throwable) {
				GameRegistryResponse rval = new GameRegistryResponse();
				
				switch (throwable.getClass().getName()) {
				// TODO Set rval.responsetype accordingly based on throwable's class name and info
				}
				
				handlers.gameRegistryResponseHandler.handle(rval);
			}
		}
	}
}