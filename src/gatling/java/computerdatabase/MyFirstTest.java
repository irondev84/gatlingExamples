package computerdatabase;

import java.time.Duration;
import java.util.*;

import io.gatling.javaapi.core.*;
import io.gatling.javaapi.http.*;
import io.gatling.javaapi.jdbc.*;

import static io.gatling.javaapi.core.CoreDsl.*;
import static io.gatling.javaapi.http.HttpDsl.*;
import static io.gatling.javaapi.jdbc.JdbcDsl.*;

public class MyFirstTest extends Simulation {

    private HttpProtocolBuilder httpProtocol = http
            .baseUrl("https://computer-database.gatling.io")
            .inferHtmlResources(AllowList(), DenyList(".*\\.js", ".*\\.css", ".*\\.gif", ".*\\.jpeg", ".*\\.jpg", ".*\\.ico", ".*\\.woff", ".*\\.woff2", ".*\\.(t|o)tf", ".*\\.png", ".*\\.svg", ".*detectportal\\.firefox\\.com.*"))
            .acceptHeader("text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,*/*;q=0.8")
            .acceptEncodingHeader("gzip, deflate, br")
            .acceptLanguageHeader("pl,en-US;q=0.7,en;q=0.3")
            .upgradeInsecureRequestsHeader("1")
            .userAgentHeader("Mozilla/5.0 (Macintosh; Intel Mac OS X 10.15; rv:109.0) Gecko/20100101 Firefox/116.0");

    FeederBuilder.Batchable searchFeeder =
            csv("data/search.csv").random();

    ChainBuilder searchForComputer =
            exec(
            http("Load computer list page")
                    .get("/computers"))
                    .pause(1)
            .feed(searchFeeder)
            .exec(http("Search computers_#{searchCriterion}")
                    .get("/computers?f=#{searchCriterion}")
                    .check(css("a:contains('#{searchComputerName}')", "href")
                            .saveAs("computerURL")))
                    .pause(1)
            .exec(http("Load computer details_#{searchComputerName}")
                    .get("#{computerURL}"))
                    .pause(2);

    FeederBuilder.Batchable computerFeeder =
            csv("data/computers.csv").circular();

    ChainBuilder addNewComputer =
            exec(
            http("Add new computer form")
                    .get("/computers/new"))
                    .pause(1)
                    .feed(computerFeeder)
            .exec(http("Create a computer_#{computerName}")
                        .post("/computers")
                        .formParam("name", "#{computerName}")
                        .formParam("introduced", "#{introduced}")
                        .formParam("discontinued", "#{discontinued}")
                        .formParam("company", "#{companyId}")
                    .check(status().is(200))
            );

    ChainBuilder deleteComputer =
            exec(
            http("Delete computer")
                    .post("/computers/381/delete"))
                    .pause(1);

    ChainBuilder browseComputers =
            exec(
                http("Browse computers")
                .get("/computers?p=0&s=name&d=desc&f=d"))
                .pause(1);

    ChainBuilder browse =
            repeat(10, "n").on(
                    exec(http("Page #{n}")
                            .get("/computers?p=#{n}"))
                            .pause(1)
            );


    private ScenarioBuilder admins = scenario("Admins")
            .exec(searchForComputer, browseComputers, browse, addNewComputer, deleteComputer);

    private  ScenarioBuilder users = scenario("Users")
            .exec(searchForComputer, browse);

    {
        setUp(admins.injectOpen(atOnceUsers(1)),
                users.injectOpen(atOnceUsers(1)))
                .protocols(httpProtocol);
    }
}
