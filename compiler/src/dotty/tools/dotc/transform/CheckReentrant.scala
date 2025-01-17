package dotty.tools.dotc
package transform

import core._
import dotty.tools.dotc.transform.MegaPhase._
import Flags._
import Contexts.Context
import Symbols._
import Decorators._


/** A no-op transform that checks whether the compiled sources are re-entrant.
 *  If -Ycheck:reentrant is set, the phase makes sure that there are no variables
 *  that are accessible from a global object. It excludes from checking paths that
 *  are labeled with one of the annotations
 *
 *      @sharable   Indicating a class or val can be safely shared
 *      @unshared   Indicating an object will not be accessed from multiple threads
 *
 *  Currently the analysis is only intended to check the dotty compiler itself. To make
 *  it generally useful we'd need to add at least the following:
 *
 *   - Handle polymorphic instantiation: We might instantiate a generic class
 *     with a type that contains vars. If the class contains fields of the generic
 *     type, this may constitute a path to a shared var, which currently goes undetected.
 *   - Handle arrays: Array elements are currently ignored because they are often used
 *     in an immutable way anyway. To do better, it would be helpful to have a type
 *     for immutable array.
 */
class CheckReentrant extends MiniPhase {
  import ast.tpd._

  override def phaseName: String = "checkReentrant"

  private[this] var shared: Set[Symbol] = Set()
  private[this] var seen: Set[ClassSymbol] = Set()
  private[this] var indent: Int = 0

  private val sharableAnnot = new CtxLazy(given ctx =>
    ctx.requiredClass("scala.annotation.internal.sharable"))
  private val unsharedAnnot = new CtxLazy(given ctx =>
    ctx.requiredClass("scala.annotation.internal.unshared"))

  def isIgnored(sym: Symbol)(implicit ctx: Context): Boolean =
    sym.hasAnnotation(sharableAnnot()) ||
    sym.hasAnnotation(unsharedAnnot()) ||
    sym.owner == defn.EnumValuesClass
      // enum values are initialized eagerly before use
      // in the long run, we should make them vals

  def scanning(sym: Symbol)(op: => Unit)(implicit ctx: Context): Unit = {
    ctx.log(i"${"  " * indent}scanning $sym")
    indent += 1
    try op
    finally indent -= 1
  }

  def addVars(cls: ClassSymbol)(implicit ctx: Context): Unit = {
    if (!seen.contains(cls) && !isIgnored(cls)) {
      seen += cls
      scanning(cls) {
        for (sym <- cls.classInfo.decls) {
          if (sym.isTerm && !sym.isSetter && !isIgnored(sym)) {
            if (sym.is(Mutable)) {
              ctx.error(
                i"""possible data race involving globally reachable ${sym.showLocated}: ${sym.info}
                   |  use -Ylog:checkReentrant+ to find out more about why the variable is reachable.""")
              shared += sym
            }
            else if (!sym.is(Method) || sym.isOneOf(Accessor | ParamAccessor))
              scanning(sym) {
                sym.info.widenExpr.classSymbols.foreach(addVars)
              }
          }
        }
        for (parent <- cls.classInfo.classParents)
          addVars(parent.classSymbol.asClass)
      }
    }
  }

  override def transformTemplate(tree: Template)(implicit ctx: Context): Tree = {
    if (ctx.settings.YcheckReentrant.value && tree.symbol.owner.isStaticOwner)
      addVars(tree.symbol.owner.asClass)
    tree
  }
}
