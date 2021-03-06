package marem.karaf.sandbox.rest.route;

import org.apache.camel.Exchange;
import org.apache.camel.PropertyInject;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.model.rest.RestOperationParamDefinition;
import org.apache.camel.model.rest.RestOperationResponseMsgDefinition;
import org.apache.camel.model.rest.RestParamType;

import java.net.InetAddress;

/**
 * Expose a rest api to handle messages.
 *
 * @author marem
 * @since 28/12/17.
 */
public class RestMessage extends RouteBuilder {

    /**
     * Http port.
     */
    @PropertyInject("rest.message.http.port")
    private String httpPort;

    /**
     * Context under which api is exposed.
     */
    @PropertyInject("rest.message.server.api.context")
    private String contextPath;

    /**
     * Context under which api swagger documentation is exposed.
     */
    @PropertyInject("rest.message.server.api.doc.context")
    private String apiDocumentationContextPath;

    @Override
    public void configure() throws Exception {
        onException(IllegalArgumentException.class)
                .routeId("badFormattedMessage")
                .maximumRedeliveries(0)
                .setHeader(Exchange.HTTP_RESPONSE_CODE, constant("400")).id("setHttpResponse400");
        onException(Exception.class)
                .log("Error");

        restConfiguration().component("servlet")
                .port(8181)
                .endpointProperty("servletName", "api-servlet")
                .enableCORS(true)
                .dataFormatProperty("prettyPrint", "true")
                .contextPath("/api")
                .apiContextPath("/doc/message")
                .apiContextRouteId("restMessageApiDocumentation")
                .apiProperty("api.version", "1.0.0")
                .apiProperty("api.title", "Marem Rest message")
                .apiProperty("api.description", "Rest service to manage messages.")
                .apiProperty("api.contact.name", "Matthieu Rémy");

        RestOperationParamDefinition bodyParam = new RestOperationParamDefinition();
        bodyParam.setDescription("The payload of the message");
        bodyParam.setName("body");
        bodyParam.setType(RestParamType.body);

        RestOperationResponseMsgDefinition acceptedResponse = new RestOperationResponseMsgDefinition();
        acceptedResponse.setCode("202");
        acceptedResponse.setMessage("Accepted. Message has been forwarded to receiver.");

        RestOperationResponseMsgDefinition invalidInputResponse = new RestOperationResponseMsgDefinition();
        invalidInputResponse.setCode("400");
        invalidInputResponse.setMessage("Invalid input data");

        RestOperationResponseMsgDefinition internalServerErrorResponse = new RestOperationResponseMsgDefinition();
        internalServerErrorResponse.setCode("500");
        internalServerErrorResponse.setMessage("Internal server error");

        String hostname = InetAddress.getLocalHost().getHostName();

        rest().path("/message")
                .consumes("application/xml,application/json,text/plain")
                .produces("application/xml,application/json")

                .get()
                    .description("GET messages")
                    .responseMessage(acceptedResponse)
                    .to("direct:getMessages")


                .post("{type}")
                    .description("POST messages of given type")
                    .param(bodyParam)
                    .responseMessage(acceptedResponse)
                    .responseMessage(invalidInputResponse)
                    .responseMessage(internalServerErrorResponse)
                    .route().routeId("restMessageApi")
                    .to("direct:postMessage");

        from("direct:getMessages")
            .setHeader(Exchange.HTTP_RESPONSE_CODE, constant("202"))
            .setHeader("serverName", simple(hostname))
            .setBody(constant("Should return messages from backend\n"));

        from("direct:postMessage")
                .setHeader("serverPort", simple("" + httpPort))

                .choice()
                    .when(simple("${header.Content-Type} in 'application/json,application/xml,text/plain'"))
                        .convertBodyTo(String.class)

                .log("received message :\n${body}\n")

                .setHeader(Exchange.HTTP_RESPONSE_CODE, constant("202"))

                .setBody(constant("OK"));
    }
}
