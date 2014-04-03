package edu.macalester.jokes;

import edu.macalester.jokes.model.Joke;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.Configuration;
import spark.Filter;
import spark.Request;
import spark.Response;

import java.util.HashMap;
import java.util.Map;

import static spark.Spark.*;

public class JokeServer {

    public static void main(String[] args) {
        // Heroku passes the port to listen on in the PORT environment variable.
        // The app won't start on Heroku unless we listen on this port.
        if(System.getenv("PORT") != null)
            setPort(Integer.parseInt(System.getenv("PORT")));

        // This is an API-only server; all responses are JSON.
        before(new Filter() {
            @Override
            public void handle(Request req, Response res) {
                res.type("application/json");
            }
        });

        get(new ApiRoute("/jokes") {
            @Override
            public Object doHandle(Request req, Response res, Session session) {
                return session.createQuery("from Joke").list();
            }
        });

        get(new ApiRoute("/jokes/:id") {
            @Override
            public Object doHandle(Request req, Response res, Session session) {
                return session.get(Joke.class, Long.parseLong(req.params("id")));
            }
        });

        post(new ApiRoute("/jokes") {
            @Override
            public Object doHandle(Request req, Response res, Session session) {
                Joke joke = new Joke();
                joke.setSetup(req.queryParams("setup"));
                joke.setPunchline(req.queryParams("punchline"));
                session.save(joke);

                Map<String,Object> resBody = new HashMap<String, Object>();
                resBody.put("success", true);
                resBody.put("joke", joke);
                return resBody;
            }
        });
    }

}
