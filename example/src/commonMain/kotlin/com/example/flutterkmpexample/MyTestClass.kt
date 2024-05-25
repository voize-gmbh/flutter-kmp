package com.example.flutterkmpexample

import de.voize.flutterkmp.annotation.FlutterFlow
import de.voize.flutterkmp.annotation.FlutterMethod
import de.voize.flutterkmp.annotation.FlutterModule
import de.voize.flutterkmp.annotation.FlutterStateFlow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.serialization.Serializable
import kotlin.time.Duration.Companion.seconds
import kotlinx.serialization.SerialName
import kotlin.time.Duration

@Serializable
data class MyDataClass(
    val stringProp: String,
    val nullableStringProp: String?,
    val intProp: Int,
    val nullableIntProp: Int?,
    val boolProp: Boolean,
    val nullableBoolProp: Boolean?,
    val floatProp: Float,
    val nullableFloatProp: Float?,
    val doubleProp: Double,
    val nullableDoubleProp: Double?,
    val nestedClassProp: NestedDataClass,
    val nullableNestedClassProp: NestedDataClass?,
    val listProp: List<String>,
    val nullableListProp: List<String>?,
    val listOfNullableProp: List<String?>,
    val mapProp: Map<String, Int>,
    val nullableMapProp: Map<String, Int>?,
    val mapOfNullableProp: Map<String, Int?>,
    val nullableWithDefault: String? = null,
) {
    @Serializable
    data class NestedDataClass(
        val stringProp: String,
        val intProp: Int,
        val boolProp: Boolean,
        val doubleProp: Double,
    )
}

@Serializable
enum class MyEnum {
    CASE_1,
    CASE_2,
    CASE_3
}
   
@Serializable
object MyDataObject

@Serializable
sealed class MySealedData {
    
    @Serializable
    @SerialName("option1")
    data class Option1(
        val name: String,
        val nested: Nested,
    ) : MySealedData() {
        @Serializable
        data class Nested(
            val nullable: String?
        )
    }

    @Serializable
    @SerialName("option2")
    data class Option2(
        val number: Int,
        val nonNested: MyDataClass,
    ) : MySealedData()

    @Serializable
    @SerialName("option3")
    object Option3 : MySealedData()
    
    @Serializable
    @SerialName("option4")
    data class Option4(
        val value: NestedSealedClass
    ) {
        @Serializable
        sealed class NestedSealedClass {
            @Serializable
            @SerialName("option1")
            data class Option1(
                val name: String,
                val nested: Nested,
            ) : NestedSealedClass() {
                @Serializable
                data class Nested(
                    val nullable: String?
                )
            }
        
            @Serializable
            @SerialName("option2")
            data class Option2(
                val number: Int,
                val nonNested: MyDataClass,
            ) : NestedSealedClass()
        
            @Serializable
            @SerialName("option3")
            object Option3 : NestedSealedClass()
        }
    }
}

@Serializable
sealed class MySealedDataWithProps {
    abstract val name: String

    @Serializable
    @SerialName("option1")
    data class Option1(
        override val name: String,
        val boolProp: Boolean,
    ) : MySealedDataWithProps()

    @Serializable
    @SerialName("option2")
    data class Option2(
        override val name: String,
        val number: Int,
    ) : MySealedDataWithProps()
}

@Serializable
data class MyDataClassWithSealed(
    val data: MySealedData,
    val nullableData: MySealedData?,
    val dataList: List<MySealedData>,
    val nullableDataList: List<MySealedData>?,
    val dataNestedList: List<List<MySealedData>>,
    val dataMap: Map<String, MySealedData>,
    val nullableDataMap: Map<String, MySealedData>?,
    val dataNestedMap: Map<String, Map<String, MySealedData>>,
    val dataComplex: Map<String, List<Map<String, MySealedData>>>,
)

@Serializable
data class MyDateClass(
    val date: LocalDate,
    val time: LocalTime,
    val dateTime: LocalDateTime,
    val duration: Duration,
    val instant: Instant,
)

private val myDataClassInstance = MyDataClass(
    stringProp = "Hello from Kotlin data class!",
    nullableStringProp = null,
    intProp = 123,
    nullableIntProp = null,
    boolProp = true,
    nullableBoolProp = null,
    floatProp = 123.456F,
    nullableFloatProp = null,
    doubleProp = 123.456,
    nullableDoubleProp = null,
    nestedClassProp = MyDataClass.NestedDataClass(
        stringProp = "Hello from Kotlin nested data class!",
        intProp = 456,
        boolProp = false,
        doubleProp = 456.789,
    ),
    nullableNestedClassProp = null,
    listProp = listOf("a", "b"),
    nullableListProp = null,
    listOfNullableProp = listOf(null, "a"),
    mapProp = mapOf("a" to 1, "b" to 2),
    nullableMapProp = null,
    mapOfNullableProp = mapOf("a" to 1, "b" to null),
)

@FlutterModule("MyTestClass")
class MyTestClass(coroutineScope: CoroutineScope) {
    private val _counter = MutableStateFlow(0)
    private val _myDataClassStateFlow = MutableStateFlow(myDataClassInstance)
    private val _myDataClassSharedFlow = MutableSharedFlow<MyDataClass>()

    init {
        coroutineScope.launch {
            while (true) {
                delay(5.seconds)
                _counter.value++
                _myDataClassSharedFlow.emit(myDataClassInstance)
            }
        }
    }

    @FlutterMethod
    fun unitMethod() {
        println("Hello from Kotlin unit method!")
    }
    
    @FlutterMethod
    fun stringMethod(): String = "Hello from Kotlin!!!"
    
    @FlutterMethod
    fun nullableStringMethod(): String? = null

    @FlutterMethod
    fun nullableMethod(): String? = null
    
    @FlutterMethod
    fun intMethod(): Int = 123
    
    @FlutterMethod
    fun doubleMethod(): Double = 123.456
    
    @FlutterMethod
    fun boolMethod(): Boolean = true
    
    @FlutterMethod
    fun parameterizedMethod(
        a: String,
        b: Int,
        c: Boolean,
        d: Double,
    ) {
        println("$a, $b, $c, $d")
    }

    @FlutterMethod
    fun localDateTimeMethod(localDateTime: LocalDateTime): LocalDateTime {
        return localDateTime
    }

    @FlutterMethod
    fun localTimeMethod(localTime: LocalTime): LocalTime {
        return localTime
    }

    @FlutterMethod
    fun localDateMethod(localDate: LocalDate): LocalDate {
        return localDate
    }

    @FlutterMethod
    fun durationMethod(duration: Duration): Duration {
        return duration
    }

    @FlutterMethod
    fun instantMethod(instant: Instant): Instant {
        return instant
    }

    @FlutterMethod
    fun stringListMethod(list: List<String>): List<String> {
        return list.map { it.reversed() }
    }
    
    @FlutterMethod
    fun nestedListMethod(list: List<List<String>>): List<List<String>> {
        return list
    }

    @FlutterMethod
    fun dataClassListMethod(list: List<MyDataClass>): List<MyDataClass> {
        return list
    }
    
    @FlutterMethod
    fun nestedDataClassListMethod(list: List<List<MyDataClass>>): List<List<MyDataClass>> {
        return list
    }
    
    @FlutterMethod
    fun mapMethod(map: Map<String, Int>): Map<String, Int> {
        return map
    }
    
    @FlutterMethod
    fun objectMethod(obj: MyDataObject): MyDataObject {
        return obj
    }

    @FlutterMethod
    fun sealedMethod(obj: MySealedData): MySealedData {
        return obj
    }

    @FlutterMethod
    fun dateClassMethod(obj: MyDateClass): MyDateClass {
        return obj
    }

    @FlutterMethod
    fun sealedWithPropsMethod(obj: MySealedDataWithProps): MySealedDataWithProps {
        return obj
    }

    @FlutterMethod
    fun classWithSealedPropMethod(obj: MyDataClassWithSealed): MyDataClassWithSealed {
        return obj
    }


    @FlutterMethod
    fun enumMethod(entry: MyEnum): MyEnum {
        return entry
    }
    
    @FlutterMethod
    fun enumListMethod(entries: List<MyEnum>): List<MyEnum> {
        return entries
    }

    @FlutterMethod
    fun enumMapMethod(entries: Map<String, MyEnum>): Map<String, MyEnum> {
        return entries
    }
    
    @FlutterMethod
    fun mixedMethod(map: Map<String, List<Map<String, MyDataClass>>>): Map<String, List<Map<String, MyDataClass>>> {
        return map
    }

    @FlutterMethod
    fun dataClassMethod(data: MyDataClass): MyDataClass {
        return data.copy(
            stringProp = "Hello from Kotlin data class method!",
            intProp = 456,
            boolProp = false,
            doubleProp = 456.789,
            nestedClassProp = data.nestedClassProp.copy(
                stringProp = "Hello from Kotlin nested data class method!",
                intProp = 789,
                boolProp = true,
                doubleProp = 789.123,
            ),
        )
    }
    
    @FlutterMethod
    suspend fun suspendUnitMethod() {
        delay(1.seconds)
        println("Hello from Kotlin suspend unit method!")
    }
    
    @FlutterMethod
    suspend fun suspendStringMethod(): String {
        delay(1.seconds)
        return "Hello from Kotlin suspend!!!"
    }

    @FlutterFlow
    val counterEvents: Flow<Int> = _counter

    @FlutterFlow
    val myDataClassEvents: Flow<MyDataClass> = _myDataClassSharedFlow

    @FlutterStateFlow
    val counter: Flow<Int> = _counter

    @FlutterStateFlow
    val myDataClassFlow: Flow<MyDataClass> = _myDataClassStateFlow

    @FlutterStateFlow
    fun myParameterizedDataClassFlow(data: MyDataClass): Flow<MyDataClass> = _myDataClassStateFlow

    @FlutterStateFlow
    fun counterPlus(num: Int): Flow<Int> = _counter.map { it + num }
}
