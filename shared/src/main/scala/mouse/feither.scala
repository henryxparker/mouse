package mouse

import cats.data.EitherT
import cats.{Applicative, FlatMap, Functor, Monad, Traverse}
import cats.instances.either._

trait FEitherSyntax {
  implicit final def FEitherSyntaxMouse[F[_], L, R](felr: F[Either[L, R]]): FEitherOps[F, L, R] =
    new FEitherOps(felr)
}

final class FEitherOps[F[_], L, R](private val felr: F[Either[L, R]]) extends AnyVal {
  def cata[A](left: L => A, right: R => A)(implicit F: Functor[F]): F[A] =
    F.map(felr)(_.fold(left, right))

  def cataF[A](left: L => F[A], right: R => F[A])(implicit F: FlatMap[F]): F[A] =
    F.flatMap(felr)(_.fold(left, right))

  def flatMapIn[A >: L, B](f: R => Either[A, B])(implicit F: Functor[F]): F[Either[A, B]] =
    F.map(felr)(_.flatMap(f))

  def flatMapF[A >: L, B](f: R => F[Either[A, B]])(implicit F: Monad[F]): F[Either[A, B]] =
    F.flatMap(felr) {
      case l @ Left(_)  => F.pure(l.asInstanceOf[Left[A, B]])
      case Right(value) => f(value)
    }

  def foldIn[A](left: L => A)(right: R => A)(implicit F: Functor[F]): F[A] =
    cata(left, right)

  def foldF[A](left: L => F[A])(right: R => F[A])(implicit F: FlatMap[F]): F[A] =
    cataF(left, right)

  def getOrElseIn[A >: R](right: => A)(implicit F: Functor[F]): F[A] =
    F.map(felr)(_.fold(_ => right, identity))

  def getOrElseF[A >: R](right: => F[A])(implicit F: Monad[F]): F[A] =
    F.flatMap(felr)(_.fold(_ => right, F.pure))

  def leftFlatMapIn[A, B >: R](f: L => Either[A, B])(implicit F: Functor[F]): F[Either[A, B]] =
    F.map(felr) {
      case Left(value)  => f(value)
      case r @ Right(_) => r.asInstanceOf[Right[A, B]]
    }

  def leftFlatMapF[A, B >: R](f: L => F[Either[A, B]])(implicit F: Monad[F]): F[Either[A, B]] =
    F.flatMap(felr) {
      case Left(left)   => f(left)
      case r @ Right(_) => F.pure(r.asInstanceOf[Right[A, B]])
    }

  def leftMapIn[A](f: L => A)(implicit F: Functor[F]): F[Either[A, R]] =
    F.map(felr) {
      case Left(value)  => Left(f(value))
      case r @ Right(_) => r.asInstanceOf[Right[A, R]]
    }

  def leftTraverseIn[G[_], A](f: L => G[A])(implicit F: Functor[F], G: Applicative[G]): F[G[Either[A, R]]] =
    F.map(felr) {
      case Left(left)   => G.map(f(left))(Left(_))
      case r @ Right(_) => G.pure(r.asInstanceOf[Right[A, R]])
    }

  def leftTraverseF[G[_], A](f: L => G[A])(implicit F: Traverse[F], G: Applicative[G]): G[F[Either[A, R]]] =
    F.traverse(felr) {
      case Left(left)   => G.map(f(left))(Left(_))
      case r @ Right(_) => G.pure(r.asInstanceOf[Right[A, R]])
    }

  def mapIn[A](f: R => A)(implicit F: Functor[F]): F[Either[L, A]] =
    F.map(felr)(_.map(f))

  def orElseIn[A >: L, B >: R](f: => Either[A, B])(implicit F: Functor[F]): F[Either[A, B]] =
    F.map(felr) {
      case r: Right[L, R] => r: Either[A, B]
      case _              => f
    }

  def orElseF[A >: L, B >: R](f: => F[Either[A, B]])(implicit F: Monad[F]): F[Either[A, B]] =
    F.flatMap(felr) {
      case r: Right[L, R] => F.pure[Either[A, B]](r)
      case _              => f
    }

  def traverseIn[G[_]: Applicative, A](f: R => G[A])(implicit F: Functor[F]): F[G[Either[L, A]]] =
    F.map(felr)(elr => catsStdInstancesForEither[L].traverse(elr)(f))

  def traverseF[G[_]: Applicative, A](f: R => G[A])(implicit F: Traverse[F]): G[F[Either[L, A]]] =
    F.traverse(felr)(elr => catsStdInstancesForEither[L].traverse(elr)(f))

  def liftEitherT: EitherT[F, L, R] =
    EitherT(felr)

}
