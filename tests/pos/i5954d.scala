abstract class MatcherFactory1 {
  type AndNotWord
}

object MatcherFactory1 {
  import scala.quoted._

  def impl(self: Expr[MatcherFactory1#AndNotWord]) given QuoteContext =
    '{ val a: Any = $self }


  def impl2[T: Type](a: MatcherFactory1)(self: Expr[T])(implicit ev: T =:= a.AndNotWord, qctx: QuoteContext) =
    '{ val a: Any = $self }

}
