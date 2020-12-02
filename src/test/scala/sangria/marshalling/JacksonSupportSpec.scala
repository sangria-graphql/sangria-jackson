package sangria.marshalling

import scala.jdk.CollectionConverters._

import com.fasterxml.jackson.databind.{
  DeserializationFeature,
  JsonNode,
  ObjectMapper
}
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import com.fasterxml.jackson.module.scala.experimental.ScalaObjectMapper
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import sangria.marshalling.jackson._
import sangria.marshalling.testkit._

class JacksonSupportSpec
    extends AnyWordSpec
    with Matchers
    with MarshallingBehaviour
    with InputHandlingBehaviour
    with ParsingBehaviour {

  val mapper = new ObjectMapper() with ScalaObjectMapper
  mapper.registerModule(DefaultScalaModule)
  mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
  val json = mapper.getNodeFactory()

  "jackson integration" should {
    behave like `value (un)marshaller`(JacksonResultMarshaller)

    behave like `AST-based input unmarshaller`(JacksonFromInput[JsonNode])
    behave like `AST-based input marshaller`(JacksonResultMarshaller)

    behave like `input parser`(
      ParseTestSubjects(
        complex =
          """{"a": [null, 123, [{"foo": "bar"}]], "b": {"c": true, "d": null}}""",
        simpleString = "\"bar\"",
        simpleInt = "12345",
        simpleNull = "null",
        list = "[\"bar\", 1, null, true, [1, 2, 3]]",
        syntaxError = List("[123, \"FOO\" \"BAR\"")
      )
    )
  }

  val root = json.objectNode()

  val fooNode = json.objectNode()
  fooNode.setAll(
    Map(
      "foo" -> json.textNode("bar")
    ).asJava
  )

  val aNode = json.arrayNode()
  aNode.addAll(
    List(
      json.nullNode(),
      json.numberNode(123),
      json.arrayNode().addAll(List(fooNode).asJava)
    ).asJava
  )

  val bNode = json.objectNode()
  bNode.setAll(
    Map(
      "c" -> json.booleanNode(true),
      "d" -> json.nullNode()
    ).asJava
  )

  val finalObject = Map(
    "a" -> aNode,
    "b" -> bNode
  ).asJava

  root.setAll(finalObject)

  val toRender = root

  "InputUnmarshaller" should {
    "throw an exception on invalid scalar values" in {
      an[IllegalStateException] should be thrownBy
        JacksonInputUnmarshaller.getScalarValue(json.objectNode())
    }

    "throw an exception on variable names" in {
      an[IllegalArgumentException] should be thrownBy
        JacksonInputUnmarshaller.getVariableName(json.textNode("$foo"))
    }

    "render JSON values" in {
      val rendered = JacksonInputUnmarshaller.render(toRender)

      rendered should be(
        """{"a":[null,123,[{"foo":"bar"}]],"b":{"c":true,"d":null}}"""
      )
    }
  }

  "ResultMarshaller" should {
    "render pretty JSON values" in {
      val rendered = JacksonResultMarshaller.renderPretty(toRender)

      rendered.replaceAll("\r", "") should be("""{
          |  "a" : [ null, 123, [ {
          |    "foo" : "bar"
          |  } ] ],
          |  "b" : {
          |    "c" : true,
          |    "d" : null
          |  }
          |}""".stripMargin.replaceAll("\r", ""))
    }

    "render compact JSON values" in {
      val rendered = JacksonResultMarshaller.renderCompact(toRender)

      rendered should be(
        """{"a":[null,123,[{"foo":"bar"}]],"b":{"c":true,"d":null}}"""
      )
    }
  }
}
