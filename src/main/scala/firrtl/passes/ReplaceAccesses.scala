// See LICENSE for license details.

package firrtl.passes

import firrtl.ir._
import firrtl.{WSubAccess, WSubIndex}
import firrtl.Mappers._


/** Replaces constant [[firrtl.WSubAccess]] with [[firrtl.WSubIndex]]
  * TODO Fold in to High Firrtl Const Prop
  */
object ReplaceAccesses extends Pass {
  def run(c: Circuit): Circuit = {
    def onStmt(s: Statement): Statement = s map onStmt map onExp
    def onExp(e: Expression): Expression = e match {
      case WSubAccess(ex, UIntLiteral(value, _), t, g) => ex.tpe match {
        case VectorType(_, len) if (value < len) => WSubIndex(onExp(ex), value.toInt, t, g)
        case _ => e map onExp
      }
      case _ => e map onExp
    }
  
    c copy (modules = c.modules map (_ map onStmt))
  }
}
