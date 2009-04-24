import edu.uwm.cs.gll._

import org.specs._
import org.scalacheck._

object ArithmeticSpecs extends Specification with ScalaCheck with ImplicitConversions {
  import Prop._
  import StreamUtils._
  
  "arithmetic grammar" should {
    "compute FIRST set" in {
      expr.first must containAll(Set('0', '1', '2', '3', '4', '5', '6', '7', '8', '9', '-'))
    }
    
    "parse numbers" in {
      val prop = forAll { x: Int =>
        expr(x.toString.toProperStream) match {
          case Success(e, Stream()) :: Nil => e.solve == x
          case _ => false
        }
      }
      
      prop must pass
    }
    
    "parse simple addition" in {
      val prop = forAll { (x: Int, y: Int) =>
        val res = expr((x + "+" + y) toProperStream)
        
        if (x < 0) {
          res.length == 2 && res.forall {
            case Success(e @ Add(Neg(e1), e2), Stream()) => e.solve == x + y
            case Success(e @ Neg(Add(e1, e2)), Stream()) => e.solve == -(-x + y)
            case _ => false
          }
        } else {
          res match {
            case Success(e, Stream()) :: Nil => e.solve == x + y
            case _ => false
          }
        }
      }
      
      prop must pass
    }
    
    "parse simple subtraction" in {
      val prop = forAll { (x: Int, y: Int) =>
        val res = expr((x + "-" + y) toProperStream)
        
        if (x < 0) {
          res.length == 2 && res.forall {
            case Success(e @ Sub(Neg(e1), e2), Stream()) => e.solve == x - y
            case Success(e @ Neg(Sub(e1, e2)), Stream()) => e.solve == -(-x - y)
            case _ => false
          }
        } else {
          res match {
            case Success(e, Stream()) :: Nil => e.solve == x - y
            case _ => false
          }
        }
      }
      
      prop must pass
    }
    
    "parse simple multiplication" in {
      val prop = forAll { (x: Int, y: Int) =>
        val res = expr((x + "*" + y) toProperStream)
        
        if (x < 0) {
          res.length == 2 && res.forall {
            case Success(e @ Mul(Neg(e1), e2), Stream()) => e.solve == x * y
            case Success(e @ Neg(Mul(e1, e2)), Stream()) => e.solve == -(-x * y)
            case _ => false
          }
        } else {
          res match {
            case Success(e, Stream()) :: Nil => e.solve == x * y
            case _ => false
          }
        }
      }
      
      prop must pass
    }
    
    "parse simple division" in {
      val prop = forAll { (x: Int, y: Int) =>
        y != 0 ==> {
          val res = expr((x + "/" + y) toProperStream)
          
          if (x < 0) {
            res.length == 2 && res.forall {
              case Success(e @ Div(Neg(e1), e2), Stream()) => e.solve == x / y
              case Success(e @ Neg(Div(e1, e2)), Stream()) => e.solve == -(-x / y)
              case _ => false
            }
          } else {
            res match {
              case Success(e, Stream()) :: Nil => e.solve == x / y
              case _ => false
            }
          }
        }
      }
      
      prop must pass
    }
    
    "produce both associativity configurations" in {
      val res = expr("42+13+12" toProperStream) map { 
        case Success(e, Stream()) => e
        case r => fail("%s does not match the expected pattern".format(r))
      }
      
      val target = Set(Add(IntLit(42), Add(IntLit(13), IntLit(12))),
                       Add(Add(IntLit(42), IntLit(13)), IntLit(12)))
      
      Set(res:_*) mustEqual target
    }
    
    "produce both binary precedence configurations" in {
      val res = expr("42+13-12" toProperStream) map { 
        case Success(e, Stream()) => e
        case r => fail("%s does not match the expected pattern".format(r))
      }
      val target = Set(Add(IntLit(42), Sub(IntLit(13), IntLit(12))),
                       Sub(Add(IntLit(42), IntLit(13)), IntLit(12)))
      
      Set(res:_*) mustEqual target
    }
    
    "produce both unary precedence configurations" in {
      val res = expr("-42+13" toProperStream) map {
        case Success(e, Stream()) => e.solve
        case r => fail("%s does not match the expected pattern".format(r))
      }
      
      res.sort { _ < _ } mustEqual List(-55, -29)
    }
  }
  
  // %%
  
  lazy val expr: Parser[Expr] = (
      expr ~ ("+" ~> expr)   ^^ { (e1, e2) => Add(e1, e2)}
    | expr ~ ("-" ~> expr)   ^^ { (e1, e2) => Sub(e1, e2)}
    | expr ~ ("*" ~> expr)   ^^ { (e1, e2) => Mul(e1, e2)}
    | expr ~ ("/" ~> expr)   ^^ { (e1, e2) => Div(e1, e2)}
    | "-" ~> expr         ^^ Neg
    | num                 ^^ IntLit
  )
  
  lazy val num: Parser[Int] = (
      num ~ digit ^^ { (n, d) => (n * 10) + d }
    | digit
  )
  
  val digit = ("0" | "1" | "2" | "3" | "4" | "5" | "6" | "7" | "8" | "9") ^^ { _.toInt }
  
  // %%
  
  sealed trait Expr {
    val solve: Int
  }
  
  case class Add(e1: Expr, e2: Expr) extends Expr {
    val solve = e1.solve + e2.solve
  }
  
  case class Sub(e1: Expr, e2: Expr) extends Expr {
    val solve = e1.solve - e2.solve
  }
  
  case class Mul(e1: Expr, e2: Expr) extends Expr {
    val solve = e1.solve * e2.solve
  }
  
  case class Div(e1: Expr, e2: Expr) extends Expr {
    val solve = e1.solve / e2.solve
  }
  
  case class Neg(e: Expr) extends Expr {
    val solve = -e.solve
  }
  
  case class IntLit(i: Int) extends Expr {
    val solve = i
  }
}