// See LICENSE for license details.

package firrtlTests.annotationTests

import firrtl._
import firrtl.annotations.{JsonProtocol, NoTargetAnnotation}
import firrtl.ir._
import _root_.logger.{Logger, LogLevel, LogLevelAnnotation}
import org.scalatest.{FlatSpec, Matchers}

case class AnAnnotation(
    info: Info,
    cir: Circuit,
    mod: DefModule,
    port: Port,
    statement: Statement,
    expr: Expression,
    tpe: Type,
    groundType: GroundType
) extends NoTargetAnnotation

class AnnoInjector extends Transform {

  override def inputForm = ChirrtlForm
  override def outputForm = ChirrtlForm

  def execute(state: CircuitState): CircuitState = {
    // Classes defined in method bodies can't be serialized by json4s
    case class MyAnno(x: Int) extends NoTargetAnnotation
    state.copy(annotations = MyAnno(3) +: state.annotations)
  }
}

class JsonProtocolSpec extends FlatSpec with Matchers {
  "JsonProtocol" should "serialize and deserialize FIRRTL types" in {

    val circuit =
      """circuit Top: @[FPU.scala 509:25]
        |  module Top:
        |    input x: UInt
        |    output y: UInt
        |    y <= add(x, x)
        |""".stripMargin
    val cir = Parser.parse(circuit)
    val mod = cir.modules.head
    val port = mod.ports.head
    val stmt = mod.asInstanceOf[Module].body
    val expr = stmt.asInstanceOf[Block].stmts.head.asInstanceOf[Connect].expr
    val tpe = port.tpe
    val groundType = port.tpe.asInstanceOf[GroundType]
    val inputAnnos = Seq(AnAnnotation(cir.info, cir, mod, port, stmt, expr, tpe, groundType))
    val annosString = JsonProtocol.serialize(inputAnnos)
    val outputAnnos = JsonProtocol.deserialize(annosString)
    inputAnnos should be (outputAnnos)
  }

  "Annotation serialization during logging" should "not throw an exception" in {
    val circuit = Parser.parse("""
      |circuit test :
      |  module test :
      |    output out : UInt<1>
      |    out <= UInt(0)
      """.stripMargin)
    (new AnnoInjector).transform(CircuitState(circuit, ChirrtlForm))
  }
}
