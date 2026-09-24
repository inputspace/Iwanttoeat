package com.inputspace.iwanttoeat

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

data class Entry(val outcome: String)
private val Bg=Color(0xFF0B0D10)
private val Card=Color(0xFF171C22)
private val TextMain=Color(0xFFE8ECEF)
private val Accent=Color(0xFFFF8A3D)

class MainActivity: ComponentActivity(){
 override fun onCreate(savedInstanceState: Bundle?){super.onCreate(savedInstanceState);setContent{App()}}
}

@Composable fun App(){
 var page by remember{mutableStateOf("home")}
 var selected by remember{mutableStateOf("")}
 var entries by remember{mutableStateOf(listOf<Entry>())}
 var minutes by remember{mutableIntStateOf(10)}
 MaterialTheme(colorScheme=darkColorScheme(background=Bg,surface=Card,primary=Accent)){
  Surface(Modifier.fillMaxSize(),color=Bg){Box(Modifier.fillMaxSize().padding(24.dp)){
   when(page){
    "home"->Home({page="wait"},{page="insights"},{page="settings"})
    "wait"->Wait(minutes,selected,{selected=it},{page="checkin"},{page="home"})
    "checkin"->CheckIn{selected=it;page="result"}
    "result"->Result(selected,{entries=entries+Entry(selected);page="home"})
    "insights"->Insights(entries){page="home"}
    else->Settings(minutes,{minutes=it}){page="home"}
   }
  }}
 }
}

@Composable private fun Home(start:()->Unit,insights:()->Unit,settings:()->Unit){Column(Modifier.fillMaxSize(),horizontalAlignment=Alignment.CenterHorizontally){Text("I want to eat",color=TextMain,fontSize=24.sp);Spacer(Modifier.height(45.dp));Text("Feeling like eating?",color=TextMain,fontSize=20.sp);Spacer(Modifier.height(24.dp));Box(Modifier.size(200.dp).background(Accent.copy(alpha=.12f),CircleShape).clickable(onClick=start),contentAlignment=Alignment.Center){Text("I want\nto eat",color=TextMain,fontSize=22.sp)};Spacer(Modifier.weight(1f));Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){TextButton(insights){Text("Insights")};TextButton(settings){Text("Settings")}}}}

@Composable private fun Wait(minutes:Int,selected:String,choose:(String)->Unit,complete:()->Unit,cancel:()->Unit){var running by remember{mutableStateOf(false)};var left by remember{mutableIntStateOf(minutes*60)};LaunchedEffect(running){while(running&&left>0){delay(1000);left--};if(running&&left==0){running=false;complete()}};Column(Modifier.fillMaxSize(),horizontalAlignment=Alignment.CenterHorizontally){Text("Pick something else",color=TextMain,fontSize=22.sp);Spacer(Modifier.height(20.dp));if(!running){listOf("Water","Walk","Tea","Stretch").forEach{item->Surface(Modifier.fillMaxWidth().padding(vertical=4.dp).clickable{choose(item)},shape=RoundedCornerShape(12.dp),color=if(selected==item)Accent.copy(alpha=.25f) else Card){Text(item,Modifier.padding(18.dp),color=TextMain)}};Spacer(Modifier.height(16.dp));Button({left=minutes*60;running=true},enabled=selected.isNotEmpty(),modifier=Modifier.fillMaxWidth()){Text("Start")}}else{Spacer(Modifier.height(60.dp));Text("%02d:%02d".format(left/60,left%60),color=TextMain,fontSize=54.sp);Spacer(Modifier.height(20.dp));Text(selected,color=TextMain);Spacer(Modifier.weight(1f));TextButton(cancel){Text("Cancel")}}}}

@Composable private fun CheckIn(select:(String)->Unit){Column(Modifier.fillMaxSize(),horizontalAlignment=Alignment.CenterHorizontally){Spacer(Modifier.height(70.dp));Text("Still want it?",color=TextMain,fontSize=28.sp);Spacer(Modifier.height(30.dp));listOf("It passed","Something small","Properly hungry").forEach{Button({select(it)},Modifier.fillMaxWidth().padding(vertical=4.dp)){Text(it)}}}}

@Composable private fun Result(outcome:String,done:()->Unit){Column(Modifier.fillMaxSize(),horizontalAlignment=Alignment.CenterHorizontally){Spacer(Modifier.height(80.dp));Text(outcome,color=TextMain,fontSize=25.sp);Spacer(Modifier.weight(1f));Button(done,Modifier.fillMaxWidth()){Text("Done")}}}

@Composable private fun Insights(entries:List<Entry>,back:()->Unit){Column(Modifier.fillMaxSize()){Text("Insights",color=TextMain,fontSize=28.sp);Spacer(Modifier.height(20.dp));Text("Logged entries: ${entries.size}",color=TextMain);Spacer(Modifier.weight(1f));TextButton(back){Text("Back")}}}

@Composable private fun Settings(minutes:Int,setMinutes:(Int)->Unit,back:()->Unit){Column(Modifier.fillMaxSize()){Text("Settings",color=TextMain,fontSize=28.sp);Spacer(Modifier.height(20.dp));Text("Wait length: ${minutes} minutes",color=TextMain);Slider(value=minutes.toFloat(),onValueChange={setMinutes(it.toInt().coerceIn(1,60))},valueRange=1f..60f);Spacer(Modifier.weight(1f));TextButton(back){Text("Back")}}}
