package laftel.net.laftel.corutine.Basic

import kotlinx.coroutines.experimental.*
import kotlinx.coroutines.experimental.channels.*


// 1. Channel basics
fun main2223(args: Array<String>) = runBlocking {
    val channel = Channel<Int>()
    launch {
        // this might be heavy CPU-consuming computation or async logic, we'll just send five squares
        for (x in 1..5) channel.send(x * x) // 채널에 값을 모아 놓는다
    }
    // here we print five received integers:
    repeat(5) { println(channel.receive()) } // 채널에 모아 놓은 값을 하나씩 꺼내 쓴다
    println("Done!")
}
// - Transfer a stream of values 를 하기 위해서 만들어 졌다
//   즉, 연속된 값을 배열에 넣어서 배열을 하나하나 넣어 놓으면 나중에 하나하나 꺼내 쓰는 것과 비슷한거 같음
// - Channedl is comceptually very similar to BlokcingQueue.
// - send(), receive() 함수가 있다


// 2. Closing and iteration over channels
fun main3133(args: Array<String>) = runBlocking {
    val channel = Channel<Int>()
    launch {
        for (x in 1..5) channel.send(x * x)
        channel.close() // we're done sending
    }
    // here we print received values using `for` loop (until the channel is closed)
    for (y in channel) println(y)
    println("Done!")
}
// - 1번에 말한 것 처럼 channel을 반복문으로 돌면서 값을 가져 올 수 있다
// - channel이 끝나면 close로 닫아야 한(반듯이 해야 할 필요는 없다)


// 3. Building channel producers
fun CoroutineScope.produceSquares(): ReceiveChannel<Int> = produce {
    for (x in 1..5) send(x * x)
}

fun main321(args: Array<String>) = runBlocking {
    val squares = produceSquares()
    squares.consumeEach { println(it) }
    println("Done!")
}
// - producer-consumer 패턴
// - produce 를 통해서 채널에 값을 모아 놓을 수 있고 consumeEach를 통해서 값을 소비 할 수 있다

// 4. Pipielines
fun CoroutineScope.produceNumbers() = produce<Int> {
    var x = 1
    while (true) send(x++) // infinite stream of integers starting from 1
}

fun CoroutineScope.square(numbers: ReceiveChannel<Int>): ReceiveChannel<Int> = produce {
    for (x in numbers) send(x * x)
}

fun main313(args: Array<String>) = runBlocking {
    val numbers = produceNumbers() // produces integers from 1 and on
    val squares = square(numbers) // squares integers
    for (i in 1..5) println(squares.receive()) // print first five
    println("Done!") // we are done
    coroutineContext.cancelChildren() // cancel children coroutines
}
// - 파이프 라인 패턴은 한쪽 코루틴이 계속해서 값을 생성하고 다른 한쪽은 게속해서 소비만 하는 패턴이다

// 5. Prime numbers with pipeline
fun CoroutineScope.numbersFrom(start: Int) = produce<Int> {
    var x = start
    while (true) send(x++) // infinite stream of integers from start
}

fun CoroutineScope.filter(numbers: ReceiveChannel<Int>, prime: Int) = produce<Int> {
    for (x in numbers) if (x % prime != 0) send(x)
}

fun main312(args: Array<String>) = runBlocking {
    var cur = numbersFrom(2)
    for (i in 1..10) {
        val prime = cur.receive()
        println(prime)
        cur = filter(cur, prime)
    }
    coroutineContext.cancelChildren() // cancel all children to let main finish
}
// - start 같은 primie property가 있다

// 6. Fan-out
fun CoroutineScope.produceNumbers1() = produce<Int> {
    var x = 1 // start from 1
    while (true) {
        send(x++) // produce next
        delay(100) // wait 0.1s
    }
}

fun CoroutineScope.launchProcessor1(id: Int, channel: ReceiveChannel<Int>) = launch {
    for (msg in channel) {
        println("Processor #$id received $msg")
    }
}

fun main314(args: Array<String>) = runBlocking<Unit> {
    val producer = produceNumbers1()
    repeat(5) { launchProcessor1(it, producer) }
    delay(950)
    producer.cancel() // cancel producer coroutine and thus kill them all
}
// - 여러 코루틴들이 하나의 채널의 값을 가져다가 쓰는
// - producer 하나로 부터 5개의 쓰레드들이 값을 받고 있는 상황
//   따라서 5개의 쓰레들이 막 왔다 갔다 하면서 1,2,3,4,5.. 을 순서대로 받는다(돌려봐)


// 7. Fan-in(공유도)
suspend fun sendString(channel: SendChannel<String>, s: String, time: Long) {
    while (true) {
        delay(time)
        channel.send(s)
    }
}

fun main3213(args: Array<String>) = runBlocking {
    val channel = Channel<String>()
    launch { sendString(channel, "foo", 200L) }
    launch { sendString(channel, "BAR!", 500L) }
    repeat(6) {
        // receive first six
        println(channel.receive())
    }
    coroutineContext.cancelChildren() // cancel all children to let main finish
}
// - 여러 코루틴들이 하나의 채널에 값을 보내는 것
// - 공유도란 어떤 모듈을 제어하는 상위 모듈의 개수를


// 7. Buffered Channels
fun main123(args: Array<String>) = runBlocking<Unit> {
    val channel = Channel<Int>(4) // create buffered channel
    val sender = launch {
        // launch sender coroutine
        repeat(10) {
            println("Sending $it") // print before sending each element
            channel.send(it) // will suspend when buffer is full
        }
    }
    // don't receive anything... just wait....
    delay(1000)
    sender.cancel() // cancel sender coroutine
}
// - buffer가 앖는 채널들은 sender와 receiver가 만났을때 값을 transfer 한다
// 때문에 sender 나 receiver중 먼저 invoke된쪽이 반대쪽을 기다린다
// - Channel() 팩토리 함수는 버퍼사이즈를 파라미터로 받는다
// - 버퍼가 꽉 차면 blocking된다 -> 따라서 repeat 10다 못돌고 blocking 된다

// 8. Channels are fair
data class Ball(var hits: Int)

suspend fun player(name: String, table: Channel<Ball>) {
    for (ball in table) { // receive the ball in a loop
        ball.hits++
        println("$name $ball")
        delay(300) // wait a bit
        table.send(ball) // send the ball back
    }
}

fun main412(args: Array<String>) = runBlocking {
    val table = Channel<Ball>() // a shared table
    launch { player("ping", table) }
    launch { player("pong", table) }
    table.send(Ball(0)) // serve the ball
    delay(1000) // delay 1 second
    coroutineContext.cancelChildren() // game over, cancel them
}
// - FIFO 를 따른다
// - 먼저 생긴 코루틴이 채널의 첫번째 값을 가져가고, 채널의 두번째 값은 두번째로 생긴 코루틴이 가져간다


// 9. Ticker channels
fun main(args: Array<String>) = runBlocking<Unit> {
    val tickerChannel = ticker(delayMillis = 100, initialDelayMillis = 0) // create ticker channel
    var nextElement = withTimeoutOrNull(1) { tickerChannel.receive() }
    println("Initial element is available immediately: $nextElement") // initial delay hasn't passed yet

    nextElement =
            withTimeoutOrNull(50) { tickerChannel.receive() } // all subsequent elements has 100ms delay
    println("Next element is not ready in 50 ms: $nextElement")

    nextElement = withTimeoutOrNull(60) { tickerChannel.receive() }
    println("Next element is ready in 100 ms: $nextElement")

    // Emulate large consumption delays
    println("Consumer pauses for 150ms")
    delay(150)
    // Next element is available immediately
    nextElement = withTimeoutOrNull(1) { tickerChannel.receive() }
    println("Next element is available immediately after large consumer delay: $nextElement")
    // Note that the pause between `receive` calls is taken into account and next element arrives faster
    nextElement = withTimeoutOrNull(60) { tickerChannel.receive() }
    println("Next element is ready in 50ms after consumer pause in 150ms: $nextElement")

    tickerChannel.cancel() // indicate that no more elements are needed
}

