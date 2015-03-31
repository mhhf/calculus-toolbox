/*
TODO:

-- reimplement printCalcDef
-- reenable Cut

*/

import swing._
import swing.event.{ButtonClicked, MouseClicked, KeyReleased, Key}

import swing.BorderPanel.Position._

import scala.collection.JavaConversions._
import scala.collection.mutable.ListBuffer
import scala.util.parsing.json.{JSON, JSONObject, JSONArray}

import java.awt.event.MouseEvent
import javax.swing.{Icon, SpinnerNumberModel, JSpinner}
import javax.swing.filechooser.FileNameExtensionFilter

import java.io.PrintWriter

import org.scilab.forge.jlatexmath.{TeXFormula, TeXConstants, TeXIcon}

/*calc_import*/
import Parser.{parseSequent, parseFormula, parseProoftree}
import PrintCalc._
import Proofsearch.derTree

object GUI extends SimpleSwingApplication {

  val AUTO_ADD_PT = "AUTO_ADD_PT"
  val AUTO_ADD_ASSM = "AUTO_ADD_ASSM"
  var globalPrefs = scala.collection.mutable.Map[String, Boolean]()

  var saveFile:Option[java.io.File] = None

  val session = CalcSession()
  //UI elements
  val inStr = new TextField { 
    text = "a |- a"
    columns = 25
  }
  
  val parsedStr = new Label { 
    val formula = new TeXFormula("a \\vdash a")
    icon = formula.createTeXIcon(TeXConstants.STYLE_DISPLAY, 15)    
  }

  val log = new Label()
  val validPT = new Label("Valid PTwCut: ")

  val addAssmButton = new Button {
    text = "Add assm"
  }

  val addPtButton = new Button {
    text = "Add PT"
    visible = false
  }

  /*val session.removeAssmButton = new Button {
    text = "Remove assm"
    enabled = false
  }

  val session.session.loadPTButton = new Button {
    text = "Load PT"
    enabled = false
  }

  val session.session.removePTsButton = new Button {
    text = "Remove PTs"
    enabled = false
  }*/

  val editButton = new Button {
    text = "Edit"
  }

  val cutButton = new Button {
    text = "Cut"
  }

  val numberModel = new SpinnerNumberModel(5, //initial value
    0, //min
    15, //max
    1)
  val ptSearchHeightSpinner = new JSpinner(numberModel)


  //define list of assms
  /*val session.listView = new ListView[(Icon, Sequent)]() {   
    listData = session.assmsBuffer
    renderer = ListView.Renderer(_._1)
  }

  //define list of found prooftrees
  val session.ptListView = new ListView[(Icon, Prooftree)]() {   
    listData = session.ptBuffer
    renderer = ListView.Renderer(_._1)
*/
  session.ptListView.listenTo(session.ptListView.mouse.clicks)
  session.ptListView.reactions += {
    case m : MouseClicked if !session.ptListView.selection.items.isEmpty && m.clicks == 2 => 
      session.loadPT
      ptPanel.update
    case m : MouseClicked if m.peer.getButton == MouseEvent.BUTTON3 => 
      val row = session.ptListView.peer.locationToIndex(m.peer.getPoint)
      if(row != -1) session.ptListView.peer.setSelectedIndex(row)
      if(!session.ptListView.selection.items.isEmpty) popup.peer.show(m.peer.getComponent, m.peer.getX, m.peer.getY)
  }
//  }


  val popup = new PopupMenu
  val menuItem = new MenuItem(Action("Add as assm") {
    session.addAssmFromSelPT()
  })
  popup.add(menuItem);
  val menuItem2 = new MenuItem(Action("Delete") {
    session.removePTs()
    session.ptListView.revalidate()
    session.ptListView.repaint()
  })
  popup.add(menuItem2);

  val menuItem3 = new MenuItem(Action("Export to LaTeX") {
    session.exportLatexFromSelPT()

  })
  popup.add(menuItem3);

  // ptPanel stuff here
  val ptPanel = new ProofTreePanel(session)
  ptPanel.build()


  //add components to listener here

  listenTo(session.listView.keys, session.ptListView.keys, inStr.keys, addAssmButton, addPtButton, editButton, cutButton) //session.addAssmButton, session.removeAssmButton, session.removePTsButton, session.loadPTButton, session.addPtButton, 
  reactions += {
    case KeyReleased(session.listView, Key.BackSpace, _, _) => session.removeAssms
    case KeyReleased(session.listView, Key.Delete, _, _) => session.removeAssms
    case KeyReleased(session.ptListView, Key.BackSpace, _, _) => session.removePTs
    case KeyReleased(session.ptListView, Key.Delete, _, _) => session.removePTs
    case KeyReleased(session.ptListView, Key.Enter, _, _) => 
      session.loadPT
      ptPanel.update

    case KeyReleased(`inStr`, k, _, _) =>
      parseSequent(inStr.text) match {
        case Some(r) => {
          session.currentSequent = r
          val latex = sequentToString(r)
          val formula = new TeXFormula(latex)
          parsedStr.icon = formula.createTeXIcon(TeXConstants.STYLE_DISPLAY, 15)

          if(k == Key.Enter){
            println("ASCII: " + sequentToString(session.currentSequent, PrintCalc.ASCII))
            println("LATEX: " + sequentToString(session.currentSequent, PrintCalc.LATEX))
            println("ISABELLE: " + sequentToString(session.currentSequent, PrintCalc.ISABELLE))

            val currentValue:Int = (ptSearchHeightSpinner.getValue).asInstanceOf[Int] //nasty hack!!
            val currentAssm = session.assmsBuffer.toList.map({case (i,s) => Premise(s)})
            //derTree(currentValue, session.currentLocale++currentAssm, session.currentSequent) match {
            new PSDialog(depth=currentValue, locale=session.currentLocale++currentAssm, seq=session.currentSequent).pt match {
              case Some(r) =>
                session.currentPT = r
                //display prooftree r in the PTPanel
                ptPanel.update()
                log.text = "PT found!"
                validPT.text = "Valid PTwCut: " + isProofTree(session.currentLocale, session.currentPT)
                //add pt to the list of found proofs
                if(globalPrefs(AUTO_ADD_PT) == true){
                  session.addPT()
                  if(globalPrefs(AUTO_ADD_ASSM) == true) session.addAssm()
                }
                case None => Dialog.showMessage(null, "No Prooftree could be found...", "Error")
            }
          }
        }
          
        case None => ;
      }
    
    case ButtonClicked(`addPtButton`) => session.addPT()
    case ButtonClicked(`addAssmButton`) => session.addAssm()

    case ButtonClicked(`editButton`) => 
      ptPanel.edit = !ptPanel.edit
      if (ptPanel.edit) editButton.text = "Done"
      else{
        editButton.text = "Edit"
        session.addPT()
        //println(ptToString(session.currentPT))
      }

    // case ButtonClicked(`cutButton`) => new FormulaInputDialog().formula match {
    //   case Some(f) =>
    //     val currentValue:Int = (ptSearchHeightSpinner.getValue).asInstanceOf[Int] //nasty hack!!
    //     val currentAssm = session.assmsBuffer.toList.map({case (i,s) => s})
    //     val lSeq = Sequenta(ant(session.currentSequent), Structure_Formula(f))
    //     val rSeq = Sequenta(Structure_Formula(f), consq(session.currentSequent))

    //     derTree(currentValue, lSeq, currentAssm) match {
    //       case Some(resL) =>
    //         derTree(currentValue, rSeq, currentAssm) match {
    //           case Some(resR) => 
    //             session.currentPT = Cut(session.currentSequent, f, resL, resR)
    //             ptPanel.update()
    //             session.addPT()
    //           case None => 
    //             val res = Dialog.showConfirmation(cutButton, 
    //               "Right Tree not found. Should I add an assumption?", 
    //               optionType=Dialog.Options.YesNo, title="Right tree not found")
    //             if (res == Dialog.Result.Ok) {
    //               session.addAssm(rSeq)
    //               val resR = Zer( rSeq, Prem() )
    //               session.currentPT = Cut(session.currentSequent, f, resL, resR)
    //               ptPanel.update()
    //               session.addPT()
    //             }
    //         }
    //       case None =>
    //         val res = Dialog.showConfirmation(cutButton, 
    //           "Left Tree not found. Should I add an assumption?", 
    //           optionType=Dialog.Options.YesNo, title="Left tree not found")
    //         if (res == Dialog.Result.Ok) {
    //           session.addAssm(lSeq)
    //           val resL = Zer( lSeq, Prem() )
    //           derTree(currentValue, rSeq, currentAssm) match {
    //             case Some(resR) => 
    //               session.currentPT = Cut(session.currentSequent, f, resL, resR)
    //               ptPanel.update()
    //               session.addPT()
    //             case None => 
    //               val res = Dialog.showConfirmation(cutButton, 
    //                 "Right Tree not found. Should I add an assumption?", 
    //                 optionType=Dialog.Options.YesNo, title="Right tree not found")
    //               if (res == Dialog.Result.Ok) {
    //                 session.addAssm(rSeq)
    //                 val resR = Zer( rSeq, Prem() )
    //                 session.currentPT = Cut(session.currentSequent, f, resL, resR)
    //                 ptPanel.update()
    //                 session.addPT()
    //               }
    //           }
    //         }
    //     }
    //   case None => Dialog.showMessage(cutButton, "Invalid formula!", "Formula Parse Error", Dialog.Message.Error)
    // }
  }

  

  //UI function definitions
  /*def session.addPT(pt: Prooftree) = {
    val newPt = (ptToIcon(pt), pt)
    session.ptBuffer += newPt
    session.ptListView.listData = session.ptBuffer
    if (!session.session.removePTsButton.enabled) session.session.removePTsButton.enabled = true
    if (!session.session.loadPTButton.enabled) session.session.loadPTButton.enabled = true
  }

  def session.addAssm(seq:Sequent) = {
    val formula = new TeXFormula(sequentToString(seq))
    val newAssm = (formula.createTeXIcon(TeXConstants.STYLE_DISPLAY, 15), seq)

    session.assmsBuffer.find(_._2 ==seq) match {
      case Some(r) => 
      case None => 
        session.assmsBuffer += newAssm
        session.listView.listData = session.assmsBuffer
        if (!session.removeAssmButton.enabled) session.removeAssmButton.enabled = true
    }
  }

  def session.removeAssms() = {
    for (i <- session.listView.selection.items) session.assmsBuffer -= i
    session.listView.listData = session.assmsBuffer
    if (session.listView.listData.isEmpty) session.removeAssmButton.enabled = false
  }

  def session.removePTs() = {
    for (i <- session.ptListView.selection.items) session.ptBuffer -= i
    session.ptListView.listData = session.ptBuffer
    if (session.ptListView.listData.isEmpty){
      session.session.removePTsButton.enabled = false
      session.session.loadPTButton.enabled = false
    }
  }

  def session.loadPT() : Unit = {
    var sel = session.ptListView.selection.items.head
    session.currentPT = sel._2
    ptPanel.update()
    //indicate if the pt is valid
    validPT.text = "Valid PTwCut: " + isProofTreeWCut(session.currentPT)
  }

  def session.addAssmFromSelPT() : Unit = {
    var sel = session.ptListView.selection.items.head
    session.addAssm(concl(sel._2))
  }

  def ptToIcon(pt:Prooftree) : TeXIcon = {
    val formula = new TeXFormula(sequentToString(concl(pt)))
    formula.createTeXIcon(TeXConstants.STYLE_DISPLAY, 15)
  }
*/

  //UI element spositioning in the main window 
  lazy val topPanel = new BorderPanel{
    layout (inStr) = Center
    layout (parsedStr) = East

    border = Swing.EmptyBorder(10, 10, 10, 10)
  }

  lazy val assmsPanel = new BoxPanel(Orientation.Vertical){
    contents += new Label("Assms:")
    contents += new ScrollPane(session.listView)
    contents += addAssmButton//new FlowPanel(session.addAssm)//,removeAssm)
    contents += new Label("PTs:")
    contents += new ScrollPane(session.ptListView)
    contents += addPtButton//new FlowPanel(addPt,session.loadPT,session.removePTs)


    border = Swing.EmptyBorder(0, 0, 0, 10)
  }

  lazy val bottomPanel = new FlowPanel {
    contents += editButton
    contents += cutButton
    contents += new Label("PT search depth:")
    contents += Component.wrap(ptSearchHeightSpinner)
    contents += log
    contents += validPT
    border = Swing.EmptyBorder(0,0,0,0)
  }


  lazy val ui = new BorderPanel{
    layout (topPanel) = North
    layout (ptPanel) = Center
    layout (bottomPanel) = South
    layout (assmsPanel) = East

    border = Swing.EmptyBorder(0, 0, 0, 0)
  }

  def openCSFile(file:java.io.File) = {
    val jsonStr = scala.io.Source.fromFile(file).getLines.mkString
    Some(JSON.parseFull(jsonStr)) match {
      case Some(M(map))  =>
        map.get("assms") match {
          case L(assms) =>
            val ass = assms.map(parseSequent(_))
            session.clearAssms
            for (Some(a) <- ass){
              session.addAssm(a)
            }
          case _ => ;
        }
        map.get("pts") match {
          case L(pts) =>
            val ptss = pts.map(parseProoftree(_))
            session.clearPT
            for (Some(pt) <- ptss){
              session.addPT(pt)
            }
          case _ => ;
        }
      case _ => ;
    }
  }

  def saveCSFile(file:java.io.File) = {  
    Some(new PrintWriter(file)).foreach{p =>
      p.write(
        JSONObject( 
          Map( 
            "assms" -> JSONArray( session.assmsBuffer.toList.map{case (i,s) => sequentToString(s, PrintCalc.ASCII)} ),
            "pts"   -> JSONArray( session.ptBuffer.toList.map{case (i,s) => prooftreeToString(s, PrintCalc.ASCII)} )   ) )
          .toString())
      p.close
    }
  }

  def top = new MainFrame {
    title = "Min Calc Toolbox"
    contents = ui
    minimumSize = new Dimension(600,400)

    System.setProperty("apple.laf.useScreenMenuBar", "true")

    menuBar = new MenuBar {
      contents += new Menu("File") {
        contents += new MenuItem(Action("Open..."){
          val chooser = new FileChooser(new java.io.File(".")) {
            title = "Open Calc Session File"
            fileFilter = new FileNameExtensionFilter("Calculus session", "cs")
          }
          val result = chooser.showOpenDialog(null)
          if (result == FileChooser.Result.Approve) openCSFile(chooser.selectedFile)
          //println(res)
        })
        contents += new MenuItem(Action("Save") {
          println("Action '"+ title +"' invoked")
          if(saveFile == None){
            val chooser = new FileChooser(new java.io.File(".")) {
              title = "Save Calc Session File"
              fileFilter = new FileNameExtensionFilter("Calculus session", "cs")
            }
            val result = chooser.showSaveDialog(null)
            if (result == FileChooser.Result.Approve) {
              val file = if (!chooser.selectedFile.toString.endsWith(".cs")) new java.io.File(chooser.selectedFile.toString+".cs") else chooser.selectedFile
              saveFile = Some(file)
              saveCSFile(saveFile.get)
            }
          } else saveCSFile(saveFile.get)
          
        })


        contents += new MenuItem(Action("Save As...") {
          println("Action '"+ title +"' invoked")
          
          val chooser = new FileChooser(new java.io.File(".")) {
            title = "Save Calc Session File"
            fileFilter = new FileNameExtensionFilter("Calculus session", "cs")
          }
          val result = chooser.showSaveDialog(null)
          if (result == FileChooser.Result.Approve) {
              val file = if (!chooser.selectedFile.toString.endsWith(".cs")) new java.io.File(chooser.selectedFile.toString+".cs") else chooser.selectedFile
              saveFile = Some(file)
              saveCSFile(file)
          }
          
        })
        contents += new Separator
        contents += new MenuItem(Action("Quit") {
          System.exit(1)
          //accelerator = Some(KeyStroke.getKeyStroke("ctrl S"))
        })
      }
      contents += new Menu("Options") {
        val aaPT = new CheckMenuItem("Auto-add PT's") {
          //this.tooltip = tooltip; 
          selected = !addPtButton.visible
          globalPrefs += (AUTO_ADD_PT -> selected)
        }
        contents += aaPT
        val aaAssm = new CheckMenuItem("Auto-add Assms") {
          //this.tooltip = tooltip; 
          selected = false
          globalPrefs += (AUTO_ADD_ASSM -> selected)
        }
        contents += aaAssm
        listenTo(aaPT, aaAssm)
        reactions += {
          case ButtonClicked(`aaPT`) => 
            globalPrefs += (AUTO_ADD_PT -> aaPT.selected)
            addPtButton.visible = !aaPT.selected
            revalidate()
            repaint()
          case ButtonClicked(`aaAssm`) => 
            globalPrefs += (AUTO_ADD_ASSM -> aaAssm.selected)
        }

        contents += new Separator
        contents += new MenuItem(Action("Generate LaTeX calc decription file") {
          Some(new PrintWriter("calc_description.tex")).foreach{p =>
            val c_def = "" //printCalcDef()
            p.write( s"\\documentclass[12pt]{article}\n\\usepackage{bussproofs}\n\n\\begin{document}\n\n$c_def\n\n\\end{document}" )
            p.close
          }
          //accelerator = Some(KeyStroke.getKeyStroke("ctrl S"))
        })
      }
      
    }
  }
}

object M extends CC[Map[String, Any]]