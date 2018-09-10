package repo.utils

import akka.http.scaladsl.model.HttpResponse
import akka.stream.Materializer
import com.google.inject.{AbstractModule, Inject, Singleton}

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}

trait AkkaHttpClientUtils {

  /**
    *
    * @param httpResponse
    * @return
    */
  def getCookies(httpResponse: HttpResponse): Future[Seq[String]]

  /**
    *
    * @param httpResponse
    * @return
    */
  def getBody(httpResponse: HttpResponse): Future[String]
}

@Singleton
case class AkkaHttpClientUtilsImpl @Inject()(
  implicit materializer: Materializer,
  executionContext: ExecutionContext
) extends AkkaHttpClientUtils {

  override def getCookies(httpResponse: HttpResponse): Future[Seq[String]] = {
    Future {
      httpResponse.headers
        .filter(_.name == "set-cookie")
        .map(_.value)
        .head
        .split("; ")
        .toSeq
    }
  }

  override def getBody(httpResponse: HttpResponse): Future[String] = {
    httpResponse.entity.toStrict(20 seconds).map(_.data.utf8String)
  }
}

case class AkkaHttpUtilsModule() extends AbstractModule {

  override def configure(): Unit = {
    bind(classOf[AkkaHttpClientUtils]).to(classOf[AkkaHttpClientUtilsImpl])
  }
}
