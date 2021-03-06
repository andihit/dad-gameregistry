package es.us.dad.gameregistry.server.service

import com.darylteo.vertx.promises.groovy.Promise
import es.us.dad.gameregistry.server.exception.ForbiddenException
import es.us.dad.gameregistry.shared.domain.GameSession
import es.us.dad.gameregistry.server.repository.ISessionRepository
import org.vertx.groovy.core.Vertx
import org.vertx.java.core.json.JsonObject
import org.vertx.java.core.logging.Logger

class SessionService {

    private final Vertx vertx
    private final Logger logger
    private final ISessionRepository sessionRepository

    public SessionService(Vertx vertx, Logger logger, ISessionRepository sessionRepository) {
        this.vertx = vertx
        this.logger = logger
        this.sessionRepository = sessionRepository
    }

    /**
     * retrieves a game session
     * @param id session id
     * @return game session or {@code null} if game session could not be found
     */
    public Promise<GameSession> getSession(UUID id) {
        return sessionRepository.findById(id)
    }

    public Promise<List<GameSession>> findSessions(UUID id, String user) {
        return sessionRepository.find(id, user)
    }

    /**
     * initializes a new game session
     * @return new game session
     */
    public Promise<GameSession> startSession(String user, String game) {
        GameSession session = new GameSession()
        session.setId(UUID.randomUUID())
        session.setUser(user)
        session.setGame(game)
        session.setStart(new Date())

        return sessionRepository.create(session)
    }

    /**
     * finishes a game session: sets end date
     * @param user current user
     * @param id session id
     * @return updated game session or {@code null} if game session couldn't be found
     */
    public Promise<GameSession> finishSession(String user, UUID id, Map<String,Object> result) {
        Promise<GameSession> p = new Promise()

        sessionRepository.findById(id).then({ GameSession session ->
            if (!session.user.equals(user))
                throw new ForbiddenException("Only the creator of the GameSession can mark the GameSession as finished.")

            session.end = new Date()
            session.result = result
            return sessionRepository.update(session)
        }).then({ GameSession session ->
            p.fulfill(session)
        }).fail({ Exception ex ->
            p.reject(ex)
        })
        // last 2 closures shouldn't be necessary, but there is some problem with the generic type of the Promise.

        return p
    }

    /**
     * deletes a game session
     * @param user current user
     * @param id session id
     * @return true if the session is found and deleted, false otherwise
     */
    public Promise<Void> deleteSession(String user, UUID id) {
        Promise<Void> p = new Promise()

        sessionRepository.findById(id).then({ GameSession session ->
            if (!session.user.equals(user))
                throw new ForbiddenException("Only the creator of the GameSession can delete the GameSession.")
            return sessionRepository.delete(id)
        }).then({
            p.fulfill(null)
        }).fail({ Exception ex ->
            p.reject(ex)
        })
        // last 2 closures shouldn't be necessary, but there is some problem with the generic type of the Promise.

        return p
    }

    /**
     * deletes all sessions which are open longer than maxAge
     * @param maxAge maximum age in seconds
     */
    public Promise<Void> cleanup(long maxAge) {
        return sessionRepository.cleanup(maxAge)
    }

}
