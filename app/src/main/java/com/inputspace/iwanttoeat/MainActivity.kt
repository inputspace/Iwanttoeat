package com.inputspace.iwanttoeat

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*

private val Bg=Color(0xFF0B0D10)
private val Surface=Color(0xFF12161B)
private val Surface2=Color(0xFF1A2027)
private val Line=Color(0xFF252C35)
private val Primary=Color(0xFFE8ECEF)
private val Muted=Color(0xFF8A94A0)
private val Ember=Color(0xFFFF8A3D)
private val Calm=Color(0xFF4FD1B5)
private val Rest=Color(0xFF7C8CF8)

enum class Screen { HOME, WAIT, CHECKIN, OUTCOME, INSIGHTS, SETTINGS }
enum class ActivityKind(val label:String,val prompt:String) {
    WATER("Water","Go get a glass of water."),
    WALK("Walk","Go for a short walk."),
    TEA("Tea","Make some tea."),
    STRETCH("Stretch","Do a few stretches.")
}
enum class Outcome(val label:String) { PASSED("It passed"), SMALL("Something small"), HUNGRY("Properly hungry") }
enum class Reason(val label:String) { BORED("Bored"), STRESSED("Stressed"), HABIT("Habit"), TIRED("Tired"), HUNGRY("Hungry") }
data class Press(val startedAt:Long,val endedAt:Long?=null,val activity:ActivityKind?=null,val outcome:Outcome?=null,val reason:Reason?=null)

class MainActivity:ComponentActivity() {
    override fun onCreate(state:Bundle?) {
        super.onCreate(state)
        setContent { App() }
    }
}

@Composable
private fun App() {
    var screen by remember { mutableStateOf(Screen.HOME) }
    var activity by remember { mutableStateOf<ActivityKind?>(null) }
    var outcome by remember { mutableStateOf<Outcome?>(null) }
    var reason by remember { mutableStateOf<Reason?>(null) }
    var presses by remember { mutableStateOf(emptyList<Press>()) }
    var waitMinutes by remember { mutableIntStateOf(10) }

    MaterialTheme(colorScheme=darkColorScheme(background=Bg,surface=Surface,primary=Ember)) {
        Surface(Modifier.fillMaxSize(),color=Bg) {
            Box(Modifier.fillMaxSize().padding(24.dp)) {
                when(screen) {
                    Screen.HOME -> Home(presses,{activity=null;screen=Screen.WAIT},{screen=Screen.INSIGHTS},{screen=Screen.SETTINGS})
                    Screen.WAIT -> Wait(waitMinutes,activity,{activity=it},{
                        presses=presses+Press(System.currentTimeMillis(),activity=activity)
                    },{screen=Screen.CHECKIN},{screen=Screen.HOME})
                    Screen.CHECKIN -> CheckIn { outcome=it;reason=null;screen=Screen.OUTCOME }
                    Screen.OUTCOME -> OutcomeView(outcome?:Outcome.PASSED,{reason=it},{
                        if(presses.isNotEmpty()) {
                            val last=presses.lastIndex
                            presses=presses.mapIndexed{idx,p->
                                if(idx==last) p.copy(endedAt=System.currentTimeMillis(),outcome=outcome,reason=reason) else p
                            }
                        }
                        screen=Screen.HOME
                    })
                    Screen.INSIGHTS -> Insights(presses){screen=Screen.HOME}
                    Screen.SETTINGS -> Settings(waitMinutes,{waitMinutes=it},{screen=Screen.HOME})
                }
            }
        }
    }
}

@Composable private fun Header() {
    val formatter=remember{SimpleDateFormat("EEE, d MMM",Locale.getDefault())}
    Text(formatter.format(Date()),color=Muted,fontSize=12.sp,fontWeight=FontWeight.Medium)
}

@Composable private fun Home(p:List<Press>,press:()->Unit,insights:()->Unit,settings:()->Unit) {
    val passed=p.count{it.outcome==Outcome.PASSED}
    val small=p.count{it.outcome==Outcome.SMALL}
    val hungry=p.count{it.outcome==Outcome.HUNGRY}
    Column(Modifier.fillMaxSize(),horizontalAlignment=Alignment.CenterHorizontally) {
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically) {
            Header()
            AssistChip(onClick={},label={Text("Kitchen closes 19:00",fontSize=10.sp)})
        }
        Spacer(Modifier.height(45.dp))
        Text("Feeling like eating?",color=Primary,fontSize=20.sp,fontWeight=FontWeight.Medium)
        Spacer(Modifier.height(22.dp))
        Box(
            Modifier.size(200.dp).background(Ember.copy(alpha=.09f),CircleShape)
                .border(1.dp,Ember.copy(alpha=.7f),CircleShape).clickable(onClick=press),
            contentAlignment=Alignment.Center
        ) {
            Column(horizontalAlignment=Alignment.CenterHorizontally) {
                Text("I want",color=Primary,fontSize=22.sp,fontWeight=FontWeight.Medium)
                Text("to eat",color=Primary,fontSize=22.sp,fontWeight=FontWeight.Medium)
            }
        }
        Spacer(Modifier.height(28.dp))
        Row(horizontalArrangement=Arrangement.spacedBy(18.dp)) { Tally(Calm,passed);Tally(Ember,small);Tally(Rest,hungry) }
        Spacer(Modifier.weight(1f))
        Row(Modifier.fillMaxWidth().border(1.dp,Line,RoundedCornerShape(16.dp)).padding(7.dp),horizontalArrangement=Arrangement.SpaceEvenly) {
            Nav("Home",true){};Nav("Insights",false,insights);Nav("Settings",false,settings)
        }
    }
}

@Composable private fun Tally(color:Color,count:Int) {
    Column(horizontalAlignment=Alignment.CenterHorizontally) {
        Box(Modifier.size(10.dp).background(color.copy(alpha=if(count==0).28f else 1f),CircleShape))
        Text(count.toString(),color=Muted,fontSize=11.sp)
    }
}
@Composable private fun Nav(label:String,selected:Boolean,onClick:()->Unit) {
    Text(label,Modifier.clickable(onClick=onClick).padding(12.dp),color=if(selected)Primary else Muted,fontSize=12.sp)
}

@Composable private fun Wait(minutes:Int,activity:ActivityKind?,choose:(ActivityKind)->Unit,onLog:()->Unit,onComplete:()->Unit,onCancel:()->Unit) {
    var running by remember{mutableStateOf(false)}
    var start by remember{mutableLongStateOf(0L)}
    var left by remember{mutableLongStateOf(minutes*60000L)}
    LaunchedEffect(running,start,minutes) {
        if(!running)return@LaunchedEffect
        while(true) {
            left=(start+minutes*60000L-System.currentTimeMillis()).coerceAtLeast(0L)
            if(left==0L){running=false;onComplete();break}
            delay(250L)
        }
    }
    val breath by rememberInfiniteTransition(label="breath").animateFloat(
        1f,1.03f,infiniteRepeatable(tween(4000),RepeatMode.Reverse),label="scale"
    )
    Column(Modifier.fillMaxSize(),horizontalAlignment=Alignment.CenterHorizontally) {
        Header()
        Spacer(Modifier.height(18.dp))
        if(!running) {
            Text("Pick something else for ten minutes.",color=Primary,fontSize=18.sp,fontWeight=FontWeight.Medium)
            Spacer(Modifier.height(18.dp))
            Column(Modifier.fillMaxWidth(),verticalArrangement=Arrangement.spacedBy(9.dp)) {
                ActivityKind.values().forEach{k->
                    Surface(Modifier.fillMaxWidth().clickable{choose(k)},shape=RoundedCornerShape(12.dp),color=if(activity==k)Surface2 else Surface) {
                        Row(Modifier.padding(17.dp),horizontalArrangement=Arrangement.SpaceBetween) {
                            Text(k.label,color=Primary);Text(if(activity==k)"Selected" else "Tap",color=Muted,fontSize=12.sp)
                        }
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
            Button({if(activity!=null){start=System.currentTimeMillis();left=minutes*60000L;running=true;onLog()}},enabled=activity!=null,modifier=Modifier.fillMaxWidth().height(52.dp)) { Text("Start the ten minutes") }
        } else {
            Countdown(left,minutes*60000L,breath)
            Spacer(Modifier.height(20.dp))
            Text(activity?.prompt?:"",color=Primary,fontSize=15.sp)
            Spacer(Modifier.weight(1f))
            Text("Cancel",Modifier.clickable(onClick=onCancel).padding(12.dp),color=Muted)
        }
    }
}

@Composable private fun Countdown(leftMs:Long,totalMs:Long,scale:Float) {
    val seconds=(leftMs/1000).coerceAtLeast(0L)
    val total=(totalMs/1000).coerceAtLeast(1L)
    val progress=(1f-seconds.toFloat()/total).coerceIn(0f,1f)
    Box(Modifier.size(240.dp).scale(scale),contentAlignment=Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val stroke=Stroke(10.dp.toPx(),cap=StrokeCap.Round)
            drawArc(Line,-90f,360f,false,style=stroke)
            drawArc(Ember,-90f,progress*360f,false,style=stroke)
        }
        Text(String.format(Locale.getDefault(),"%02d:%02d",seconds/60,seconds%60),color=Primary,fontSize=56.sp,fontWeight=FontWeight.SemiBold)
    }
}

@Composable private fun CheckIn(select:(Outcome)->Unit) {
    Column(Modifier.fillMaxSize(),horizontalAlignment=Alignment.CenterHorizontally) {
        Spacer(Modifier.height(70.dp))
        Text("Still want it?",color=Primary,fontSize=28.sp,fontWeight=FontWeight.SemiBold)
        Spacer(Modifier.height(34.dp))
        Outcome.values().forEach{o->
            Button({select(o)},Modifier.fillMaxWidth().height(56.dp).padding(bottom=8.dp),colors=ButtonDefaults.buttonColors(containerColor=Surface2)) { Text(o.label,color=Primary) }
        }
    }
}

@Composable private fun OutcomeView(o:Outcome,setReason:(Reason)->Unit,done:()->Unit) {
    LaunchedEffect(o){if(o!=Outcome.SMALL){delay(3000L);done()}}
    Column(Modifier.fillMaxSize(),horizontalAlignment=Alignment.CenterHorizontally) {
        Spacer(Modifier.height(58.dp))
        when(o) {
            Outcome.PASSED->{Text("✓",color=Calm,fontSize=70.sp);Text("Nice. That one's gone.",color=Primary,fontSize=22.sp)}
            Outcome.SMALL->{Text("Something small",color=Primary,fontSize=24.sp,fontWeight=FontWeight.SemiBold);Spacer(Modifier.height(18.dp));Surface(Modifier.fillMaxWidth(),shape=RoundedCornerShape(16.dp),color=Surface){Column(Modifier.padding(20.dp),verticalArrangement=Arrangement.spacedBy(10.dp)){Text("Serve it on a plate or katori.",color=Primary);Text("Sit at the table.",color=Primary);Text("No screen while you eat.",color=Primary)}}}
            Outcome.HUNGRY->Text("Go eat. Enjoy it.",color=Primary,fontSize=24.sp,fontWeight=FontWeight.SemiBold)
        }
        Spacer(Modifier.height(28.dp))
        Text("Reason (optional)",color=Muted,fontSize=13.sp)
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(4.dp)) { Reason.values().forEach{r->AssistChip({setReason(r)},label={Text(r.label,fontSize=10.sp)})} }
        Spacer(Modifier.weight(1f))
        if(o==Outcome.SMALL) Button(done,Modifier.fillMaxWidth().height(52.dp)){Text("Got it")} else Text("No guilt. Just data.",color=Muted,fontSize=12.sp)
    }
}

@Composable private fun Insights(p:List<Press>,back:()->Unit) {
    Column(Modifier.fillMaxSize()) {
        Text("Insights",color=Primary,fontSize=28.sp,fontWeight=FontWeight.SemiBold);Spacer(Modifier.height(22.dp))
        if(p.size<5) Text("Press the button a few times and patterns will show up here.",color=Muted,fontSize=14.sp)
        else {
            val resolved=p.filter{it.outcome!=null};val pass=resolved.count{it.outcome==Outcome.PASSED};val rate=if(resolved.isEmpty())0 else pass*100/resolved.size
            Text("This week's pass rate",color=Muted,fontSize=13.sp);Text("$rate%",color=Primary,fontSize=56.sp,fontWeight=FontWeight.SemiBold)
            Spacer(Modifier.height(22.dp));Text("Top reasons",color=Muted,fontSize=13.sp)
            Reason.values().forEach{r->Row(Modifier.fillMaxWidth().padding(vertical=6.dp),horizontalArrangement=Arrangement.SpaceBetween){Text(r.label,color=Primary);Text(p.count{it.reason==r}.toString(),color=Muted)}}
        }
        Spacer(Modifier.weight(1f));Text("Back",Modifier.clickable(onClick=back).padding(12.dp),color=Muted)
    }
}

@Composable private fun Settings(wait:Int,setWait:(Int)->Unit,back:()->Unit) {
    var sound by remember{mutableStateOf(false)}
    Column(Modifier.fillMaxSize()) {
        Text("Settings",color=Primary,fontSize=28.sp,fontWeight=FontWeight.SemiBold);Spacer(Modifier.height(25.dp))
        Text("Wait length",color=Muted,fontSize=13.sp)
        Row(verticalAlignment=Alignment.CenterVertically) {
            Slider(Modifier.weight(1f),wait.toFloat(),{setWait(it.toInt().coerceIn(1,60))},valueRange=1f..60f)
            Spacer(Modifier.width(10.dp));Text("${wait}m",color=Primary)
        }
        Spacer(Modifier.height(14.dp))
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically) {
            Column{Text("Sound",color=Primary);Text("Soft chime at the end",color=Muted,fontSize=12.sp)}
            Switch(sound,{sound=it})
        }
        Spacer(Modifier.height(20.dp))
        Text("Meal windows: 07:30–10:00, 13:00–14:00, 18:00–19:00. Kitchen close: 19:00.",color=Muted,fontSize=12.sp)
        Spacer(Modifier.weight(1f));Text("Back",Modifier.clickable(onClick=back).padding(12.dp),color=Muted)
    }
}
