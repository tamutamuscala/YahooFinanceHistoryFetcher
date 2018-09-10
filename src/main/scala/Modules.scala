import java.io.File

import akka.actor.{ActorRefFactory, ActorSystem}
import akka.stream.{ActorMaterializer, ActorMaterializerSettings, Materializer}
import com.google.inject._
import com.typesafe.config.{Config, ConfigFactory}
import repo.StockDaoModule
import repo.utils.AkkaHttpUtilsModule

import scala.concurrent.ExecutionContext

// injector
//TODO: change name to application injector
object Injector {
  val globalInjector = Guice.createInjector(
    ConfigModule("resources/application.conf"),
    ActorSystemModule(ActorSystem("GLOBAL")),
    AkkaHttpUtilsModule(),
    StockDaoModule()
  )
}

case class ConfigModule(configFilePath: String) extends AbstractModule {

  override def configure(): Unit = {}

  @Provides
  def provideConfig(): Config = {
    val configFile = ConfigFactory.parseFile(new File(configFilePath))
    ConfigFactory.load(configFile)
  }
}

case class ActorSystemModule(actorSystem: ActorSystem) extends AbstractModule {

  override def configure(): Unit = {}

  @Provides
  def provideActorRefFactory(): ActorRefFactory = {
    actorSystem
  }

  @Provides
  def provideActorSystem(): ActorSystem = {
    actorSystem
  }

  @Provides
  def provideDispatcher(): ExecutionContext = {
    actorSystem.dispatcher
  }

  @Provides
  def provideMaterializer(): Materializer = {
    provideActorMaterializer()
  }

  @Provides
  def provideActorMaterializer(): ActorMaterializer = {
    ActorMaterializer(ActorMaterializerSettings(actorSystem))(actorSystem)
  }
}
