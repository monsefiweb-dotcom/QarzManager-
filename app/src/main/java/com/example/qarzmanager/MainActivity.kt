package com.example.qarzmanager

import android.app.*
import android.os.Bundle
import android.content.*
import android.graphics.Color
import android.view.Gravity
import android.widget.*
import java.util.*

data class Debt(
    val id: Long,
    var person: String,
    var amount: Double,
    var paid: Double,
    var type: Int,
    val date: Long,
    var note: String
)

class MainActivity : Activity() {
    private val prefs by lazy { getSharedPreferences("qarz_db", MODE_PRIVATE) }
    private val debts = mutableListOf<Debt>()
    private var language = 0
    private lateinit var root: LinearLayout

    private fun tr(ps:String, fa:String, en:String) = when(language) {
        0 -> ps
        1 -> fa
        else -> en
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        load()
        home()
    }

    private fun load() {
        debts.clear()
        val raw = prefs.getString("debts", "") ?: return
        if (raw.isBlank()) return
        raw.split(";;").forEach { x ->
            val a=x.split("|")
            if(a.size>=7) try {
                debts.add(Debt(a[0].toLong(),a[1],a[2].toDouble(),a[3].toDouble(),
                    a[4].toInt(),a[5].toLong(),a[6]))
            } catch(_:Exception){}
        }
    }

    private fun save() {
        prefs.edit().putString("debts", debts.joinToString(";;") {
            "${it.id}|${it.person}|${it.amount}|${it.paid}|${it.type}|${it.date}|${it.note}"
        }).apply()
    }

    private fun page(title:String) {
        root=LinearLayout(this)
        root.orientation=LinearLayout.VERTICAL
        root.setPadding(18,22,18,18)
        root.setBackgroundColor(Color.WHITE)
        val h=TextView(this)
        h.text=title; h.textSize=25f; h.gravity=Gravity.CENTER
        root.addView(h, LinearLayout.LayoutParams(-1,75))
        setContentView(root)
    }

    private fun button(text:String, action:()->Unit) {
        val b=Button(this)
        b.text=text; b.textSize=16f; b.setOnClickListener{action()}
        root.addView(b, LinearLayout.LayoutParams(-1,66).apply{setMargins(0,5,0,5)})
    }

    private fun home() {
        page(tr("د پورونو حساب","مدیریت قرض‌ها","Debt Manager"))
        val mine=debts.filter{it.type==0}.sumOf{it.amount-it.paid}
        val owed=debts.filter{it.type==1}.sumOf{it.amount-it.paid}
        val summary=TextView(this)
        summary.text=tr(
            "زما باندې پورونه: %.2f\nپر ما باندې پورونه: %.2f\nپاتې ټول پور: %.2f".format(mine,owed,mine+owed),
            "قرض‌های من: %.2f\nقرض به من: %.2f\nمجموع باقی: %.2f".format(mine,owed,mine+owed),
            "My debts: %.2f\nOwed to me: %.2f\nTotal remaining: %.2f".format(mine,owed,mine+owed)
        )
        summary.textSize=18f; summary.setPadding(12,8,12,18); root.addView(summary)

        button(tr("＋ نوی پور ثبت کړه","＋ ثبت قرض جدید","＋ New Debt")){addDebt()}
        button(tr("👥 ټول حسابونه","👥 همه حساب‌ها","👥 All Accounts")){accounts()}
        button(tr("📅 د نن ورځې حساب","📅 حساب امروز","📅 Today")){report(true)}
        button(tr("📆 د دې میاشتې حساب","📆 حساب این ماه","📆 This Month")){report(false)}
        button(tr("🔎 لټون","🔎 جستجو","🔎 Search")){search()}
        button(tr("🌐 ژبه","🌐 زبان","🌐 Language")){language()}
    }

    private fun addDebt() {
        page(tr("نوی پور ثبت کړه","ثبت قرض جدید","New Debt"))
        val name=EditText(this); name.hint=tr("د شخص نوم","نام شخص","Person name"); root.addView(name)
        val amount=EditText(this); amount.hint=tr("د پور اندازه","مبلغ قرض","Amount"); amount.inputType=2; root.addView(amount)
        val type=Spinner(this)
        type.adapter=ArrayAdapter(this,android.R.layout.simple_spinner_dropdown_item,
            arrayOf(tr("زما باندې پورونه","قرض‌های من","I Owe"),
                    tr("پر ما باندې پورونه","قرض به من","Owed to Me")))
        root.addView(type)
        val note=EditText(this); note.hint=tr("یادښت (اختیاري)","یادداشت (اختیاری)","Note (optional)"); root.addView(note)

        button(tr("ثبتول","ثبت","Save")) {
            val n=name.text.toString().trim()
            val a=amount.text.toString().toDoubleOrNull()
            if(n.isEmpty() || a==null || a<=0) {
                Toast.makeText(this,tr("سم معلومات ولیکه","اطلاعات درست وارد کنید","Enter valid information"),Toast.LENGTH_SHORT).show()
            } else {
                debts.add(Debt(System.currentTimeMillis(),n,a,0.0,type.selectedItemPosition,System.currentTimeMillis(),note.text.toString()))
                save(); home()
            }
        }
        button(tr("بېرته","برگشت","Back")){home()}
    }

    private fun accounts(list:List<Debt> = debts) {
        page(tr("ټول حسابونه","همه حساب‌ها","All Accounts"))
        if(list.isEmpty()) {
            val x=TextView(this); x.text=tr("تر اوسه حساب نشته.","هنوز حسابی نیست.","No accounts yet."); x.textSize=18f; root.addView(x)
        } else list.forEach { d ->
            val b=Button(this)
            b.text="${d.person}\n${tr("پاتې","باقی","Remaining")}: ${"%.2f".format(d.amount-d.paid)}"
            b.setOnClickListener{detail(d)}
            root.addView(b,LinearLayout.LayoutParams(-1,75).apply{setMargins(0,4,0,4)})
        }
        button(tr("بېرته","برگشت","Back")){home()}
    }

    private fun detail(d:Debt) {
        page(d.person)
        val tv=TextView(this)
        tv.text=tr(
            "ټول پور: ${d.amount}\nورکړل شوي: ${d.paid}\nپاتې: ${d.amount-d.paid}\nیادښت: ${d.note}",
            "کل قرض: ${d.amount}\nپرداخت شده: ${d.paid}\nباقی: ${d.amount-d.paid}\nیادداشت: ${d.note}",
            "Debt: ${d.amount}\nPaid: ${d.paid}\nRemaining: ${d.amount-d.paid}\nNote: ${d.note}"
        ); tv.textSize=19f; tv.setPadding(8,10,8,20); root.addView(tv)

        button(tr("＋ ورکړه ثبت کړه","＋ ثبت پرداخت","＋ Add Payment")){payment(d)}
        button(tr("✏ د پور معلومات بدل کړه","✏ ویرایش","✏ Edit")){editDebt(d)}
        button(tr("🗑 حذف","🗑 حذف","🗑 Delete")) {
            AlertDialog.Builder(this).setTitle(tr("حذف؟","حذف؟","Delete?"))
                .setMessage(tr("دا حساب حذف شي؟","این حساب حذف شود؟","Delete this account?"))
                .setPositiveButton(tr("هو","بلی","Yes")){_,_->debts.remove(d);save();accounts()}
                .setNegativeButton(tr("نه","نخیر","No"),null).show()
        }
        button(tr("بېرته","برگشت","Back")){accounts()}
    }

    private fun payment(d:Debt) {
        val e=EditText(this); e.inputType=2; e.hint=tr("د ورکړې اندازه","مبلغ پرداخت","Payment amount")
        AlertDialog.Builder(this).setTitle(tr("ورکړه","پرداخت","Payment")).setView(e)
            .setPositiveButton(tr("ثبت","ثبت","Save")){_,_->
                e.text.toString().toDoubleOrNull()?.let { p->
                    d.paid=(d.paid+p).coerceAtMost(d.amount); save(); detail(d)
                }
            }.setNegativeButton(tr("لغوه","لغو","Cancel"),null).show()
    }

    private fun editDebt(d:Debt) {
        val e=EditText(this); e.setText(d.person); e.hint=tr("نوم","نام","Name")
        AlertDialog.Builder(this).setTitle(tr("ویرایش","ویرایش","Edit")).setView(e)
            .setPositiveButton(tr("ثبت","ثبت","Save")){_,_->d.person=e.text.toString();save();detail(d)}
            .setNegativeButton(tr("لغوه","لغو","Cancel"),null).show()
    }

    private fun report(today:Boolean) {
        page(if(today) tr("د نن ورځې حساب","حساب امروز","Today") else tr("د دې میاشتې حساب","حساب این ماه","This Month"))
        val now=Calendar.getInstance()
        val list=debts.filter {
            val c=Calendar.getInstance(); c.timeInMillis=it.date
            if(today) c.get(Calendar.YEAR)==now.get(Calendar.YEAR)&&c.get(Calendar.DAY_OF_YEAR)==now.get(Calendar.DAY_OF_YEAR)
            else c.get(Calendar.YEAR)==now.get(Calendar.YEAR)&&c.get(Calendar.MONTH)==now.get(Calendar.MONTH)
        }
        val tv=TextView(this); tv.textSize=19f
        tv.text=tr("ثبتونه: ${list.size}\nټول پور: ${list.sumOf{it.amount}}\nورکړل شوي: ${list.sumOf{it.paid}}\nپاتې: ${list.sumOf{it.amount-it.paid}}",
            "ثبت‌ها: ${list.size}\nکل قرض: ${list.sumOf{it.amount}}\nپرداخت: ${list.sumOf{it.paid}}\nباقی: ${list.sumOf{it.amount-it.paid}}",
            "Entries: ${list.size}\nDebt: ${list.sumOf{it.amount}}\nPaid: ${list.sumOf{it.paid}}\nRemaining: ${list.sumOf{it.amount-it.paid}}")
        root.addView(tv); button(tr("بېرته","برگشت","Back")){home()}
    }

    private fun search() {
        page(tr("لټون","جستجو","Search"))
        val e=EditText(this); e.hint=tr("د شخص نوم ولیکه","نام شخص را وارد کنید","Enter person name"); root.addView(e)
        button(tr("لټون","جستجو","Search")) {
            accounts(debts.filter{it.person.contains(e.text.toString(),true)})
        }
        button(tr("بېرته","برگشت","Back")){home()}
    }

    private fun language() {
        page(tr("ژبه وټاکئ","انتخاب زبان","Choose Language"))
        button("پښتو"){language=0;home()}
        button("دری"){language=1;home()}
        button("English"){language=2;home()}
    }
}