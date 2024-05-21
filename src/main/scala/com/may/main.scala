package com.may

import cats.effect.{IO, IOApp}
import org.legogroup.woof.{*, given}
import Logger.*
import cats.effect.kernel.Resource
import fs2.Stream
import org.http4s.server.Server
import com.comcast.ip4s.*
import org.http4s.HttpRoutes
import org.http4s.ember.server.EmberServerBuilder
import cats.syntax.all.*
import io.github.liquibase4s.LiquibaseConfig
import org.http4s.dsl.io.*

import java.sql.{Connection, DriverManager}

object main extends IOApp.Simple:
  case class Agent(accountId: Long, agent: Long)

  given Filter = Filter.everything
  given Printer = ColorPrinter()

  def run: IO[Unit] =
    for
      given Logger[IO] <- DefaultLogger.makeIo(Output.fromConsole)
      _                <- runWithLogger.withLogContext("application", "app").withLogContext("environment", "dev")
    yield ()

  private def runWithLogger(using logger: Logger[IO]): IO[Unit] = for {
    _ <- logger.info(s"Starting app")
    _ <- Stream
      .resource(resources)
      .flatMap { httpServer =>
        Stream.eval(logger.warn("Before exception")) ++
        Stream.eval(httpServer.useForever).concurrently {
          throw new RuntimeException("Catch me and log to console")
        }
      }
      .handleErrorWith { e =>
        Stream.eval {
          logger.error(s"An error occured: $e") // TODO: doesn't print anything
        }
      }
      .compile
      .drain
  } yield ()

  private def resources(using logger: Logger[IO]): Resource[IO, Resource[IO, Server]] =
    for
      _ <- Resource.eval(Logger[IO].info(s"Initializing resources"))
      config <- Resource.eval(initLiquibaseConfig())
      conn <- Resource.eval(getConnection(config))
      httpServer = EmberServerBuilder
        .default[IO]
        .withHost(host"0.0.0.0")
        .withPort(port"9006")
        .withHttpApp(
          HttpRoutes.of[IO] {
            case GET -> Root / "health" => Ok("works")
            case _ => Ok("works")
          }.orNotFound
        )
        .build
    yield httpServer

  private def initLiquibaseConfig(): IO[LiquibaseConfig] = 
    for {
      config <- IO(LiquibaseConfig(
        url = "jdbc:mysql://broken-host-name/somedb",
        user = "root",
        password = "master",
        driver = "com.mysql.cj.jdbc.Driver",
        changelog = "changelog.sql"
      ))
    } yield config

  private def getConnection(config: LiquibaseConfig): IO[Connection] =
    Class.forName(config.driver)
    IO(DriverManager.getConnection(config.url, config.user, config.password))
