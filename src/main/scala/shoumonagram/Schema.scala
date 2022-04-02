package shoumonagram

import sangria.schema._

object SchemaDefinition {
  val PostType = ObjectType(
    "Post",
    "A Shoumonagram Post",
    fields[Unit, Post](
      Field("id", IntType, resolve = _.value.id),
      Field("body", StringType, resolve = _.value.body),
      Field("userId", IntType, resolve = _.value.userId)
    )
  )

  val CountArg = Argument("count", IntType)
  val QueryType = ObjectType(
    "Query",
    fields[PostRepository, Unit](
      Field(
        "latestPosts",
        ListType(PostType),
        description = Some(""),
        arguments = CountArg :: Nil,
        resolve = c => c.ctx.latestPosts(c arg CountArg)
      )
    )
  )
  val PostInputType: InputType[Post] = InputObjectType[Post](
    "PostInput",
    "A Post to be created",
    List(
      InputField("id", IntType, defaultValue = 0),
      InputField("body", StringType),
      InputField("userId", IntType)
    )
  )
  val BodyArg = Argument("body", StringType)
  val MutationType = ObjectType(
    "Mutation",
    fields[PostRepository, Unit](
      Field(
        "addPost",
        PostType,
        description = Some(""),
        arguments = BodyArg :: Nil,
        resolve = c => c.ctx.addPost(c.args.arg("body"), 1)
      )
    )
  )

  val schema = Schema(QueryType, Some(MutationType))
}
