package scalafiddle.client

import diode._
import japgolly.scalajs.react._
import japgolly.scalajs.react.extra.router._
import org.scalajs.dom

import scalafiddle.client.component.FiddleEditor
import scalafiddle.shared.FiddleId

sealed trait Page

object AppRouter {

  case object Home extends Page

  case class EditorPage(id: String, version: Int) extends Page

  val fiddleData = AppCircuit.connect(_.fiddleData)

  val config = RouterConfigDsl[Page].buildConfig { dsl =>
    import dsl._

    (staticRoute(root, Home) ~> render(fiddleData(d => FiddleEditor(d, None, AppCircuit.zoom(_.compilerData), AppCircuit.zoom(_.loginData))))
      | dynamicRouteCT("sf" / (string("\\w+") / int).caseClass[EditorPage]) ~> dynRender(
      (p: EditorPage) => {
        val fid = FiddleId(p.id, p.version)
        AppCircuit.dispatch(UpdateId(fid, true))
        fiddleData(d => FiddleEditor(d, Some(fid), AppCircuit.zoom(_.compilerData), AppCircuit.zoom(_.loginData)))
      })
      )
      .notFound(redirectToPage(Home)(Redirect.Replace))
  }.renderWith(layout)

  val baseUrl =
    if (dom.window.location.hostname == "localhost")
      BaseUrl.fromWindowOrigin_/
    else
      BaseUrl.fromWindowOrigin_/

  val (router, routerLogic) = Router.componentAndLogic(baseUrl, config)

  def layout(c: RouterCtl[Page], r: Resolution[Page]) = {
    import japgolly.scalajs.react.vdom.all._

    div(r.render()).render
  }

  def navigated(page: ModelRO[Page]): Unit = {
    // scalajs.js.timers.setTimeout(0)(routerLogic.ctl.set(page.value).runNow())
    page() match {
      case Home =>
        scalajs.js.timers.setTimeout(0)(dom.window.location.assign("/"))
      case EditorPage(id, version) =>
        scalajs.js.timers.setTimeout(0)(dom.window.location.assign(s"/sf/$id/$version"))
    }
  }

  // subscribe to navigation changes
  AppCircuit.subscribe(AppCircuit.zoom(_.navLocation))(navigated)
}
