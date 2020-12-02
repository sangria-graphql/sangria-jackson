package sangria.marshalling

import scala.jdk.CollectionConverters._
import scala.math.{BigDecimal, BigInt}
import scala.util.Try

import com.fasterxml.jackson.databind.node.{
  ArrayNode,
  JsonNodeFactory,
  NullNode,
  ObjectNode,
  TextNode,
  ValueNode
}
import com.fasterxml.jackson.databind.{
  DeserializationFeature,
  JsonNode,
  ObjectMapper
}
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import com.fasterxml.jackson.module.scala.experimental.ScalaObjectMapper
import sangria.marshalling._

object jackson {
  private object util {
    val mapper = new ObjectMapper() with ScalaObjectMapper
    mapper.registerModule(DefaultScalaModule)
    mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
    val json: JsonNodeFactory = mapper.getNodeFactory
  }

  implicit object JacksonResultMarshaller extends ResultMarshaller {
    import util._

    type Node = JsonNode
    type MapBuilder = ArrayMapBuilder[Node]

    def emptyMapNode(keys: Seq[String]) = new ArrayMapBuilder[Node](keys)
    def addMapNodeElem(
        builder: MapBuilder,
        key: String,
        value: Node,
        optional: Boolean
    ): ArrayMapBuilder[JsonNode] = builder.add(key, value)

    def mapNode(builder: MapBuilder): ObjectNode = {
      val node = json.objectNode()
      node.setAll(builder.toMap.asJava)
      node
    }

    def mapNode(keyValues: Seq[(String, JsonNode)]): ObjectNode = {
      val node = json.objectNode()
      node.setAll(keyValues.toMap.asJava)
      node
    }

    def arrayNode(values: Vector[JsonNode]): ArrayNode =
      json.arrayNode().addAll(values.asJava)

    def optionalArrayNodeValue(value: Option[JsonNode]): JsonNode =
      value match {
        case Some(v) => v
        case None    => nullNode
      }

    def scalarNode(
        value: Any,
        typeName: String,
        info: Set[ScalarValueInfo]
    ): ValueNode =
      value match {
        case v: String     => json.textNode(v)
        case v: Boolean    => json.booleanNode(v)
        case v: Int        => json.numberNode(v)
        case v: Long       => json.numberNode(v)
        case v: Float      => json.numberNode(v)
        case v: Double     => json.numberNode(v)
        case v: BigInt     => json.numberNode(v.bigInteger)
        case v: BigDecimal => json.numberNode(v.bigDecimal)
        case v =>
          throw new IllegalArgumentException("Unsupported scalar value: " + v)
      }

    def enumNode(value: String, typeName: String): TextNode =
      json.textNode(value)

    def nullNode: NullNode = json.nullNode()

    def renderCompact(node: JsonNode): String = node.toString
    def renderPretty(node: JsonNode): String = node.toPrettyString
  }

  implicit object JacksonMarshallerForType
      extends ResultMarshallerForType[JsonNode] {
    val marshaller: JacksonResultMarshaller.type = JacksonResultMarshaller
  }

  implicit object JacksonInputUnmarshaller extends InputUnmarshaller[JsonNode] {
    import util._

    private def findNodeOpt(node: JsonNode, key: String): Option[JsonNode] = {
      val nodeOrMissing = node.asInstanceOf[ObjectNode].findPath(key)
      if (nodeOrMissing.isMissingNode) {
        None
      } else {
        Some(nodeOrMissing)
      }
    }

    def getRootMapValue(node: JsonNode, key: String): Option[JsonNode] =
      findNodeOpt(node, key)

    def isMapNode(node: JsonNode): Boolean = node.isObject
    def getListValue(node: JsonNode): Seq[JsonNode] = node.asScala.toList
    def isListNode(node: JsonNode): Boolean = node.isArray

    def getMapValue(node: JsonNode, key: String): Option[JsonNode] =
      findNodeOpt(node, key)

    def getMapKeys(node: JsonNode): Seq[String] =
      node.asInstanceOf[ObjectNode].fieldNames.asScala.toList

    def isDefined(node: JsonNode): Boolean =
      node != json.nullNode() && node != json.missingNode()

    def getScalarValue(node: JsonNode): Any = node match {
      case b if b.isBoolean => b.asBoolean()
      case i if i.isInt     => i.asInt()
      case d if d.isDouble  => d.asDouble()
      case l if l.isLong    => l.asLong()
      case d if d.isBigDecimal =>
        BigDecimal.javaBigDecimal2bigDecimal(d.decimalValue())
      case t if t.isTextual => t.asText()
      case b if b.isBigInteger =>
        BigInt.javaBigInteger2bigInt(b.bigIntegerValue())
      case f if f.isFloat => f.floatValue()
      case _              => throw new IllegalStateException(s"$node is not a scalar value")
    }

    def getScalaScalarValue(node: JsonNode): Any = getScalarValue(node)

    def isEnumNode(node: JsonNode): Boolean = node.isTextual

    def isScalarNode(node: JsonNode): Boolean =
      node.isBoolean || node.isNumber || node.isTextual

    def isVariableNode(node: JsonNode) = false
    def getVariableName(node: JsonNode) = throw new IllegalArgumentException(
      "variables are not supported"
    )

    def render(node: JsonNode): String = node.toString
  }

  private object JacksonToInput extends ToInput[JsonNode, JsonNode] {
    def toInput(value: JsonNode): (JsonNode, JacksonInputUnmarshaller.type) =
      (value, JacksonInputUnmarshaller)
  }

  implicit def JacksonToInput[T <: JsonNode]: ToInput[T, JsonNode] =
    JacksonToInput.asInstanceOf[ToInput[T, JsonNode]]

  private object JacksonFromInput extends FromInput[JsonNode] {
    val marshaller: JacksonResultMarshaller.type = JacksonResultMarshaller
    def fromResult(node: marshaller.Node): JsonNode = node
  }

  implicit def JacksonFromInput[T <: JsonNode]: FromInput[T] =
    JacksonFromInput.asInstanceOf[FromInput[T]]

  implicit object JacksonInputParser extends InputParser[JsonNode] {
    import util._
    def parse(str: String): Try[JsonNode] = Try(mapper.readTree(str))
  }
}
