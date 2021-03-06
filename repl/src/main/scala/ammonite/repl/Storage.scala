package ammonite.repl

import acyclic.file
import java.io.{File, FileInputStream, IOException, FileWriter}
import ammonite.repl.Util.IvyMap
import org.yaml.snakeyaml.Yaml
import scala.collection.generic.{GenericCompanion, GenericTraversableTemplate, CanBuildFrom, SeqFactory}
import scala.collection.{IterableLike, mutable, IndexedSeqLike}
import scala.collection.mutable.ArrayBuffer
import scala.collection.JavaConversions.asScalaBuffer
import ammonite.pprint

/**Trait for the interface of common persistent storage. 
 * This handles history and persistent caches.
 * Right now it is not threadsafe nor does it handle the mutual exclusion of files between processes. 
 * Mutexes should be added to be able to run multiple Ammonite processes on the same system.
 */ 
trait Storage{
  def loadPredef: String

  def loadHistory: History

  def saveHistory(h: History): Unit

  def loadIvyCache: IvyMap

  def saveIvyCache(map: IvyMap)
}

object Storage{

  def apply(dir: File) = new Storage{
  
    if(dir.exists){
      if(!dir.isDirectory){
        dir.delete()
        dir.mkdir()
      }
    } else {
      dir.mkdir()
    }

    def loadHistory: History = {
      val yaml = new Yaml
//      println("loadHistory")
      try{
        val list = yaml.load(new FileInputStream(dir + "/history"))
        list match {
          case a: java.util.List[String] => new History(a.toVector)
          case _ => new History(Vector())
        }
      } catch {
        case e: IOException => new History(Vector())
      }
    }

    def saveHistory(h: History): Unit = {
      val yaml = new Yaml
      val fw = new FileWriter(dir + "/history")
      yaml.dump(h.toArray, fw)
    }

    def loadPredef = try{
      io.Source.fromFile(dir + "/predef.scala").mkString
    }catch{
      case e: java.io.FileNotFoundException => ""
    }

    def loadIvyCache = {
      val json = try{
        io.Source.fromFile(dir + "/ivycache.json").mkString
      }catch{
        case e: java.io.FileNotFoundException => "[]"
      }

      try{
        upickle.read[IvyMap](json)
      }catch{ case e =>
        Map.empty
      }
    }
    def saveIvyCache(map: IvyMap) = {
      val fw = new FileWriter(dir + "/ivycache.json")
      fw.write(upickle.write(map))
      fw.flush()
    }
  }
}

class History(s: Vector[String])
extends IndexedSeq[String]
with IndexedSeqLike[String, History] {
  def ? = this :+ "asd"
  def length: Int = s.length
  def apply(idx: Int): String = s.apply(idx)
  override def newBuilder = History.builder
}

object History{
  def builder = new mutable.Builder[String, History] {
    val buffer = mutable.Buffer.empty[String]
    def +=(elem: String): this.type = {buffer += elem; this}

    def result(): History = new History(buffer.toVector)

    def clear(): Unit = buffer.clear()
  }
  implicit def cbf = new CanBuildFrom[History, String, History]{
    def apply(from: History) = builder
    def apply() = builder
  }
  implicit def toHistory(s: Seq[String]): History = new History(s.toVector)

  import pprint._
  implicit def historyPPrint(implicit c: Config): PPrint[History] = new PPrint(
    new PPrinter[History]{
      def render(t: History, c: Config)={
        val lines = if(c.lines() > 0) c.lines() else t.length
        val seq = "\n" +: t.dropRight(lines).flatMap{ code => Seq("@ ", code, "\n") }
        if(t.length > lines) ("\n..." +: seq).iterator
        else seq.iterator
      }
    },
    c
  )
}

