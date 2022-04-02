package shoumonagram

import scala.util.Random

object PostRepository {
  import collection.mutable.{ListBuffer => MuList}

  private var Posts = MuList(
    Post(3, "おやすみなさい", 3),
    Post(2, "こんにちは", 2),
    Post(1, "おはようございます", 1)
  )
}

class PostRepository {
  def latestPosts(count: Int): Seq[Post] =
    PostRepository.Posts.take(count).toSeq
  def addPost(body: String, userId: Int): Post = {
    // stub
    val id = Random.nextInt()
    val p = Post(id, body, userId)
    PostRepository.Posts = PostRepository.Posts.prepend(p)
    println("added post")
    return p
  }
}
