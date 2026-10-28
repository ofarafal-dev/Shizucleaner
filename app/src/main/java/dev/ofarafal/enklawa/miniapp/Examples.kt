package dev.ofarafal.enklawa.miniapp

/** Dwie przykładowe mini-aplikacje wbudowane w APK: jedna bez uprawnień, druga z magazynem danych. */
object Examples {
    private const val STYLE = "body{font:16px sans-serif;background:#10151c;color:#e4e8ec;margin:0;padding:16px}" +
        "button,input{font:inherit;padding:10px;border-radius:10px;border:1px solid #2a3342;background:#1c2530;color:#e4e8ec}" +
        "button{background:#7fdbca;color:#00201b;border:0}li{padding:8px 0;border-bottom:1px solid #2a3342}"

    val passwordGenerator: Map<String, ByteArray> = mapOf(
        "manifest.json" to """{"id":"generator-hasel","name":"Generator haseł","version":"1","permissions":[]}""",
        "index.html" to """<!doctype html><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1">
<style>$STYLE #out{font:18px monospace;word-break:break-all;margin:14px 0}</style>
<h3>Generator haseł</h3>
<label>Długość: <input id="len" type="number" value="20" min="8" max="64" style="width:80px"></label>
<div id="out"></div><button id="go">Losuj</button>
<p style="color:#a9b4bf;font-size:13px">Losowanie przez crypto.getRandomValues. Aplikacja nie ma dostępu do sieci ani do danych.</p>
<script>
const A="ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789!@#%^*-_=+";
function gen(n){const r=new Uint32Array(n);crypto.getRandomValues(r);return Array.from(r,x=>A[x%A.length]).join("")}
document.getElementById("go").onclick=()=>{document.getElementById("out").textContent=gen(+document.getElementById("len").value||20)};
document.getElementById("go").click();
</script>""",
    ).mapValues { it.value.toByteArray() }

    val todo: Map<String, ByteArray> = mapOf(
        "manifest.json" to """{"id":"zadania","name":"Zadania","version":"1","permissions":["storage"]}""",
        "index.html" to """<!doctype html><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1">
<style>$STYLE ul{list-style:none;padding:0}.done{text-decoration:line-through;color:#6b7785}</style>
<h3>Zadania</h3>
<div><input id="t" placeholder="nowe zadanie" style="width:60%"> <button id="add">Dodaj</button></div>
<ul id="l"></ul>
<p id="m" style="color:#a9b4bf;font-size:13px"></p>
<script>
const has = typeof Microcosm !== "undefined";
let items = has ? JSON.parse(Microcosm.get("items")||"[]") : [];
function save(){ if(has) Microcosm.set("items", JSON.stringify(items)); }
function draw(){
  const l=document.getElementById("l"); l.innerHTML="";
  items.forEach((it,i)=>{const li=document.createElement("li"); li.textContent=it.t; if(it.d) li.className="done";
    li.onclick=()=>{it.d=!it.d;save();draw()}; li.oncontextmenu=e=>{e.preventDefault();items.splice(i,1);save();draw()}; l.appendChild(li)});
  document.getElementById("m").textContent = has ? "Dotknij: zrobione. Przytrzymaj: usuń. Dane są w zaszyfrowanym magazynie świata." : "Brak zgody na magazyn: zadania znikną po zamknięciu okna.";
}
document.getElementById("add").onclick=()=>{const v=document.getElementById("t").value.trim(); if(v){items.push({t:v,d:false});document.getElementById("t").value="";save();draw()}};
draw();
</script>""",
    ).mapValues { it.value.toByteArray() }
}
