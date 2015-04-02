import swing.{Button, ListView, FileChooser}
import scala.collection.mutable.ListBuffer

import javax.swing.filechooser.FileNameExtensionFilter
import javax.swing.Icon

import java.io.PrintWriter

import org.scilab.forge.jlatexmath.{TeXFormula, TeXConstants, TeXIcon}

/*calc_import*/
import PrintCalc.{sequentToString, prooftreeToString}

case class CalcSession() {

	def relAKA(alpha : Action)(a : Agent)(beta: Action) : Boolean = (alpha, a, beta) match {
		case (Actiona(List('e','p')), Agenta(List('c')), Actiona(List('e','w'))) => true
		// shoudl we have this one as well? :
		case (Actiona(List('e','w')), Agenta(List('c')), Actiona(List('e','p'))) => true
		case (Actiona(x), _, Actiona(y)) => x == y
		case _ => false
	}

	var currentSequent : Sequent = Sequenta(Structure_Formula(Formula_Atprop(Atpropa(List('a')))),Structure_Formula(Formula_Atprop(Atpropa(List('a')))))
	var currentPT : Prooftree = Prooftreea( Sequenta(Structure_Formula(Formula_Atprop(Atpropa(List('a')))),Structure_Formula(Formula_Atprop(Atpropa(List('a'))))), RuleZera(Id()), List())
	var currentLocale : List[Locale] = List(Empty(), RelAKA(relAKA), Swapout(relAKA, List(Actiona(List('e','p','a'))) ))
	var currentPTsel : Option[(Icon, Prooftree)] = None

	val assmsBuffer = ListBuffer[(Icon, Sequent)]()
	val ptBuffer = ListBuffer[(Icon, Prooftree)]()

	val listView = new ListView[(Icon, Sequent)]() {   
    	listData = assmsBuffer
    	renderer = ListView.Renderer(_._1)
  	}
  	val ptListView = new ListView[(Icon, Prooftree)]() {   
    	listData = ptBuffer
    	renderer = ListView.Renderer(_._1)
    }

	/*val addAssmButton = new Button {
		text = "Add assm"
	}
	val removeAssmButton = new Button {
		text = "Remove assm"
		enabled = false
	}

	val addPtButton = new Button {
		text = "Add PT"
		visible = false
	}
	val loadPTButton = new Button {
		text = "Load PT"
		enabled = false
	}
	val removePTsButton = new Button {
		text = "Remove PTs"
		enabled = false
	}*/

    def addAssm(seq:Sequent = currentSequent) = {
		val formula = new TeXFormula(sequentToString(seq))
		val newAssm = (formula.createTeXIcon(TeXConstants.STYLE_DISPLAY, 15), seq)

		assmsBuffer.find(_._2 ==seq) match {
			case Some(r) => 
			case None => 
				assmsBuffer += newAssm
				listView.listData = assmsBuffer
				//if (!removeAssmButton.enabled) removeAssmButton.enabled = true
		}
	}
	def removeAssms() = {
		for (i <- listView.selection.items) assmsBuffer -= i
		listView.listData = assmsBuffer
		//if (listView.listData.isEmpty) removeAssmButton.enabled = false
	}

	def clearAssms() = {
		assmsBuffer.clear()
	}

    def addPT(pt: Prooftree = currentPT) = {
		val newPt = (ptToIcon(pt), pt)
		ptBuffer += newPt
		ptListView.listData = ptBuffer
		currentPTsel = Some(newPt)

		//if (!removePTsButton.enabled) removePTsButton.enabled = true
		//if (!loadPTButton.enabled) loadPTButton.enabled = true
	}

	def savePT(ptSel: Option[(Icon, Prooftree)] = currentPTsel, pt : Prooftree = currentPT) = ptSel match {
		case Some(sel) =>
			// if delete or add below was used, we want a new pt....
			if (concl(sel._2) == concl(pt)){
				val newPt = (sel._1, pt)
				val index = ptBuffer.indexOf(sel)
				ptBuffer.update(index, newPt)
				ptListView.listData = ptBuffer
			} else {
				addPT(pt)
			}
		case None => addPT(pt)
		//if (!removePTsButton.enabled) removePTsButton.enabled = true
		//if (!loadPTButton.enabled) loadPTButton.enabled = true
	}

	def loadPT() = {
		var sel = ptListView.selection.items.head
		currentPTsel = Some(sel)
		currentPT = sel._2
	}
	def removePTs() = {
		for (i <- ptListView.selection.items) ptBuffer -= i
		ptListView.listData = ptBuffer
		/*if (ptListView.listData.isEmpty){
			removePTsButton.enabled = false
			loadPTButton.enabled = false
		}*/
	}

	def clearPT() = {
		ptBuffer.clear()
	}

	def addAssmFromSelPT() : Unit = {
		var sel = ptListView.selection.items.head
		addAssm(concl(sel._2))
	}

	def exportLatexFromSelPT() : Unit = {
		var sel = ptListView.selection.items.head

		val chooser = new FileChooser(new java.io.File(".")) {
			title = "Save LaTeX File"
			fileFilter = new FileNameExtensionFilter("LaTeX (.tex)", "tex")
		}
		val result = chooser.showSaveDialog(null)
		if (result == FileChooser.Result.Approve) {
			val file = if (!chooser.selectedFile.toString.endsWith(".tex")) chooser.selectedFile.toString+".tex" else chooser.selectedFile.toString
			Some(new PrintWriter(file)).foreach{p =>
		    	p.write(prooftreeToString(sel._2) + "\\DisplayProof")
		    	p.close
		    }
		}

	}

	def ptToIcon(pt:Prooftree) : TeXIcon = {
		new TeXFormula(sequentToString(concl(pt))).createTeXIcon(TeXConstants.STYLE_DISPLAY, 15)
	}

	def findMatches(seq: Sequent) : List[Prooftree] = for {
		(i, pt) <- ptBuffer.toList
		if concl(pt) == seq
	} yield pt

	def mergePTs(repPt: Prooftree, insertPoint:SequentInPt, root:SequentInPt, children: SequentInPt => Iterable[SequentInPt]) : Prooftree = {
	    if(insertPoint == root) return repPt
	    return Prooftreea( root.seq, root.rule, children(root).toList.map(x => mergePTs(repPt, insertPoint, x, children)) )
	}

	def deleteAbove(deletePoint:SequentInPt, root:SequentInPt, children: SequentInPt => Iterable[SequentInPt]) : Prooftree = {
	    if(deletePoint == root) return Prooftreea(root.seq, RuleZera(Prem()), List())
	    return Prooftreea( root.seq, root.rule, children(root).toList.map( x => deleteAbove(deletePoint, x, children) ) )
	}

	def rebuildFromPoint(root:SequentInPt, children: SequentInPt => Iterable[SequentInPt]) : Prooftree = 
		return Prooftreea( root.seq, root.rule, children(root).toList.map( x => rebuildFromPoint(x, children) ) )

}