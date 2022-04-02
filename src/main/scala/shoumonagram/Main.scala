package shoumonagram

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives._
import spray.json.JsValue
import sangria.parser.QueryParser
import spray.json.JsObject
import spray.json.JsString
import scala.util.Success
import scala.util.Failure
import sangria.ast.Document
import sangria.marshalling.sprayJson

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.Http
import ch.megard.akka.http.cors.scaladsl.CorsDirectives._

case class Post(id: Int, body: String, userId: Int)

object Main extends App with SprayJsonSupport {
  import sangria.marshalling.sprayJson._

  // -- HTTP Server
  implicit val system = ActorSystem("shoumonagram")
  implicit val materializer = ActorMaterializer()

  import system.dispatcher

  val route =
    cors() {
      (post & path("graphql")) {
        entity(as[JsValue]) { json =>
          graphQLEndpoint(json)
        }
      } ~
        (get & path("graphiql") & pathEndOrSingleSlash) {
          getFromResource("graphiql.html")
        }
    }

  Http().bindAndHandle(route, "0.0.0.0", 8080)

  def graphQLEndpoint(requestJson: JsValue) = {
    val JsObject(fields) = requestJson
    val Some(JsString(query)) =
      fields.get("query") orElse fields.get("mutation")
    val op = fields.get("operationName") collect { case JsString(op) =>
      op
    }
    val vars = fields.get("variables") match {
      case Some(obj: JsObject) => obj
      case _                   => JsObject.empty
    }

    QueryParser.parse(query) match {
      // query parsed successfully, time to execute it!
      case Success(queryAst) =>
        complete(executeGraphQLQuery(queryAst, op, vars))
      // can't parse GraphQL query, return error
      case Failure(error) =>
        complete(
          StatusCodes.BadRequest,
          JsObject("error" -> JsString(error.getMessage))
        )
    }
  }

  def executeGraphQLQuery(
      query: Document,
      op: Option[String],
      vars: JsObject
  ) = {
    import sangria.execution._
    import StatusCodes.{OK, BadRequest, InternalServerError}
    Executor
      .execute(
        SchemaDefinition.schema,
        query,
        new PostRepository,
        variables = vars,
        operationName = op
      )
      .map(OK -> _)
      .recover {
        case err: QueryAnalysisError => BadRequest -> err.resolveError
        case err: ErrorWithResolver =>
          InternalServerError -> err.resolveError
      }
  }
}
