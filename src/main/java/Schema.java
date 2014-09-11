import org.vertx.java.core.Handler;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.platform.Verticle;

public class Schema extends Verticle {

    public void start() {
        vertx.eventBus().registerHandler("schema-version", new Handler<Message<String>>() {
            @Override
            public void handle(Message<String> message) {
                message.reply("0.0.0");
                container.logger().info("Schema returned version");
            }
        });
        container.logger().info("Schema verticle started");
    }
}
