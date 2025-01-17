import scala.util.FromDigits
object Test extends App {

  val x: BigInt = 13232202002020202020202
  val y: BigInt = -0xaabb12345ACF12345AC
  val z: BigDecimal = 132322020020.223

  case class Even(n: Int)

  given as FromDigits[Even] {
    def fromDigits(digits: String): Even = {
      val intValue = digits.toInt
      if (intValue % 2 == 0) Even(intValue)
      else throw FromDigits.MalformedNumber()
    }
  }

  val e: Even = 1234

  println(x)
  println(y)
  println(z)
  println(e)

  try println(123: Even)
  catch {
    case ex: FromDigits.MalformedNumber => println("malformed")
  }

  val N = 10

  x match {
    case 13_232_202_002_020_202_020_202 => ()
  }
  (x: Any) match {
    case 13232202002020202020202: BigInt => ()
  }
  y match {
    case 13232202002020202020202 => assert(false)
    case -0xaabb12345ACF12345AC => ()
  }
  z match {
    case 132322020020.223 => ()
  }
  (z: Any) match {
    case 132_322_020_020.223: BigDecimal => ()
  }

  e match {
    case 1234 =>
  }
  (e: Any) match {
    case 12: Even => assert(false)
    case 1234: Even =>
    case _: Even =>
  }
}