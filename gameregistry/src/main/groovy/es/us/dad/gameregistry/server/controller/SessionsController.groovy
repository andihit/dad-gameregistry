package es.us.dad.gameregistry.server.controller

import com.hazelcast.util.StringUtil
import es.us.dad.gameregistry.server.exception.BadRequestException
import es.us.dad.gameregistry.shared.domain.GameSession
import es.us.dad.gameregistry.server.exception.MethodNotAllowedException
import es.us.dad.gameregistry.server.service.ILoginService
import es.us.dad.gameregistry.server.service.SessionService
import es.us.dad.gameregistry.server.util.*
import io.netty.handler.codec.http.HttpResponseStatus
import org.vertx.groovy.core.http.HttpServerRequest
import org.vertx.java.core.json.JsonObject

class SessionsController extends Controller {

    private final SessionService sessionService

    public SessionsController(ILoginService loginService, SessionService sessionService) {
        super(loginService)
        this.sessionService = sessionService
    }

    @GET("/api/v1/sessions")
    public void getSessions(HttpServerRequest request) {
        String user = request.params.get("user")

        sessionService.findSessions(null, user).then({ List<GameSession> sessions ->
            sendJsonResponse(request, [count: sessions.size(), sessions: sessions.collect { it.toJsonMap() }])
        }).fail({ Exception ex ->
            sendErrorResponse(request, ex)
        })
    }

    @Authenticated
    @POST("/api/v1/sessions")
    public void createSession(HttpServerRequest request) {
        String user = getCurrentUser(request)

        getRequestBody(request).then({ JsonObject body ->
            String game = body.getString("game")
            if (game == null || game.isEmpty())
                throw new BadRequestException("""The game name is missing in the request body. Please supply a JSON in the request body, ex. {"game": "test game"}""")
            return sessionService.startSession(user, game)
        }).then({ GameSession newSession ->
            sendJsonResponse(request, newSession, HttpResponseStatus.CREATED)
        }).fail({ Exception ex ->
            sendErrorResponse(request, ex)
        })
    }

    @PUT("/api/v1/sessions")
    public void changeSessions(HttpServerRequest request) {
        sendErrorResponse(request, new MethodNotAllowedException())
    }

    @DELETE("/api/v1/sessions")
    public void deleteSessions(HttpServerRequest request) {
        sendErrorResponse(request, new MethodNotAllowedException())
    }

}
