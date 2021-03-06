package es.us.dad.gameregistry.server.controller

import es.us.dad.gameregistry.server.service.ILoginService
import es.us.dad.gameregistry.server.service.StaticFilesService
import org.vertx.groovy.core.http.HttpServerRequest
import org.vertx.groovy.core.http.RouteMatcher
import org.vertx.java.core.logging.Logger

class StaticFilesController extends Controller {
    final private StaticFilesService fileService
    final private String base_path
    final private Logger logger

    public StaticFilesController(ILoginService loginService, StaticFilesService fileService, Logger logger, String base_path) {
        super(loginService)
        this.fileService = fileService
        this.logger = logger

        if (base_path[base_path.length() - 1] == '/')
            this.base_path = base_path.substring(0, base_path.length() - 1)
        else
            this.base_path = base_path
    }

    public void registerUrls(RouteMatcher routeMatcher) {
        String regexp = "^" + base_path.replaceAll("\\/", "\\\\\\/") + "\\/(.*)"
        //String regexp = "\\/doc\\/(.*)"
        logger.info("Static file server bounded the server's resource path '${fileService.getWebRoot()}' to '${base_path}'.")
        routeMatcher.allWithRegEx(regexp, { HttpServerRequest request ->
            String withoutBasePath = request.path.substring(base_path.length())

            fileService.getSystemPathOf(withoutBasePath).then({ String system_path ->
                request.response.sendFile(system_path)
            }).fail({ Throwable ex ->
                request.response.setStatusCode(404)
                        .end()
            })
        })
    }
}
