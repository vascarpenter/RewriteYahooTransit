import org.jsoup.Jsoup
import java.awt.Font
import java.awt.Toolkit
import java.awt.datatransfer.*
import java.io.*
import java.util.*
import javax.swing.JFrame
import javax.swing.UIManager

fun main(args:Array<String>)
{
    val f = JFrame("Reformat Yahoo Transit 1.0")
    val g = gui()
    UIManager.put("OptionPane.messageFont", Font("Dialog", Font.PLAIN, 12))
    UIManager.put("OptionPane.buttonFont", Font("Dialog", Font.PLAIN, 12))
    f.apply {
        defaultCloseOperation = JFrame.EXIT_ON_CLOSE
        contentPane = g.panel
        setSize(600, 300)
        isResizable = false
        setLocationRelativeTo(null)
        isVisible = true
    }

    g.convertButton.addActionListener {
        convertButton(g)
    }
}


/**
 * システムクリップボードにtextをコピーする.
 *
 * @param text コピーする文字列
 */
fun setClipboardString(text: String?)
{
    val kit: Toolkit = Toolkit.getDefaultToolkit()
    val clip: Clipboard = kit.systemClipboard
    val ss = StringSelection(text)
    clip.setContents(ss, ss)
}

/**
 * クリップボードの内容 (TEXT) を返します。
 * @return クリップボードの内容 text
 */
fun getClipboardString(): String
{
    val kit = Toolkit.getDefaultToolkit()
    val clip = kit.systemClipboard
    val contents = clip.getContents(null)
    var result = ""
    val hasTransferableText = (contents != null
            && contents.isDataFlavorSupported(DataFlavor.stringFlavor))
    if (hasTransferableText) {
        try {
            result = contents
                .getTransferData(DataFlavor.stringFlavor) as String
        } catch (ex: UnsupportedFlavorException) {
            // highly unlikely since we are using a standard DataFlavor
            println(ex)
            ex.printStackTrace()
        } catch (ex: IOException) {
            println(ex)
            ex.printStackTrace()
        }
    }
    return result
}

/**
 * 指定したデリミタで文字列を分割し、 Stringの配列で取得することができるメソッド.
 *
 * @param value     分割対象文字列
 * @param delimiter デリミタ
 * @return 分割されたString配列
 */
fun stringToArray(
    value: String,
    delimiter: String
): Array<String>
{
    val list = mutableListOf <String>()
    val stt = StringTokenizer(value, delimiter)
    while (true)
    {
        if (!stt.hasMoreTokens()) break
        val word = stt.nextToken()
        list.add(word)
    }
    return list.toTypedArray()
}

/**
 * newTxt内にエラーメッセージを設定する.
 */
fun notTransitData(g: gui)
{
    g.textArea1.text = "Yahoo路線情報からsafari/chromeでコピーしたデータではありません。<br>" +
            "ルートXを含めたところにマウスカーソルをおき、" +
            "到着までの範囲をコピーしてから変換ボタンを押してください。"
}

private class HtmlSelection(private val html: String) : Transferable
{
    companion object
    {
        private val htmlFlavors: ArrayList<DataFlavor> = ArrayList<DataFlavor>()

        init
        {
            try
            {
                htmlFlavors.add(
                    DataFlavor(
                        "text/html;class=java.lang.String"
                    )
                )
                htmlFlavors
                    .add(DataFlavor("text/html;class=java.io.Reader"))
                htmlFlavors.add(
                    DataFlavor(
                        "text/html;charset=unicode;class=java.io.InputStream"
                    )
                )
            } catch (ex: ClassNotFoundException)
            {
                ex.printStackTrace()
            }
        }
    }

    override fun getTransferDataFlavors(): Array<DataFlavor>
    {
        return htmlFlavors.toTypedArray()
    }

    override fun isDataFlavorSupported(flavor: DataFlavor): Boolean
    {
        return htmlFlavors.contains(flavor)
    }

    @Throws(UnsupportedFlavorException::class)
    override fun getTransferData(flavor: DataFlavor): Any
    {
        return when
        {
            String::class.java == flavor.representationClass -> html
            Reader::class.java == flavor.representationClass -> StringReader(html)
            InputStream::class.java == flavor.representationClass -> StringBufferInputStream(html)
            else -> throw UnsupportedFlavorException(flavor)
        }
    }
}

fun setClipboardHTMLString(text: String) {
    val kit = Toolkit.getDefaultToolkit()
    val clip = kit.systemClipboard
    val t: Transferable = HtmlSelection(text)
    clip.setContents(t, null)
}

/**
 * フィールドを変換する このアプリケーションの主体.
 */
fun convertButton(g: gui)
{
    val tx: String = getClipboardString()
    var tt = ""
    val st = StringTokenizer(tx, "\n")
    if (!st.hasMoreTokens()) {
        notTransitData(g)
        return
    }
    var t2 = st.nextToken()
    if (t2.length < 4 || t2.indexOf("ルート") < 0) {
        notTransitData(g)
        return
    }
    if (!st.hasMoreTokens()) {
        notTransitData(g)
        return
    }

    // 1行飛ばす  [早][楽]
    var t = st.nextToken()
    if (!st.hasMoreTokens()) {
        notTransitData(g)
        return
    }
    t = st.nextToken()
    var ts: Array<String> = stringToArray(t, "[")
    tt += ts[0] + "<br>"

    // 2行とばす
    while (true) {
        if (!st.hasMoreTokens()) {
            notTransitData(g)
            return
        }
        t = st.nextToken()
        if (t.indexOf("share") >= 0 || t.indexOf("print") >= 0 || t.indexOf("priic") >= 0 || t.indexOf("reg") >= 0 || t.indexOf(
                "commuterpass"
            ) >= 0 || t.contains("乗換") || t.contains("km")
        ) {
            continue
        } else break
    }
    tt += t + "発 "

    // [dep]
    t = st.nextToken()
    if (t.indexOf("share") >= 0) {
        notTransitData(g)
        return
    }
    while (true)
    {
        //　駅名
        t = st.nextToken()

        ts = stringToArray(t, "\t")
        tt += ts[0]

        t = st.nextToken()  // 時刻表　地図
        // [line]
        if (!st.hasMoreTokens()) {
            notTransitData(g)
            return
        }
        t = st.nextToken()  // [line]
        // [train]ＪＲ新幹線はやぶさ105号・盛岡行
        if (!st.hasMoreTokens()) {
            notTransitData(g)
            return
        }
        t = st.nextToken()
        ts = stringToArray(t, "]")
        tt += " <font color=blue>" + ts[1] + "</font>"

        // 21番線発 / 13番線着
        if (!st.hasMoreTokens()) break
        t = st.nextToken()
        tt += " <font color=blue>$t</font>"
        //10駅
        // 指定席：6,520円
        while (true)
        {
            if (!st.hasMoreTokens()) break
            t = st.nextToken()
            if (t.contains("現金") || t.contains("指定席") || t.contains("自由席") || t.contains("特急料金")
                || t.contains("駅")  || t.contains("円"))
            {
                continue
            }
            else break
        }
        if(t.contains("着"))
        {
            // 着があれば発もある 乗り継ぎ
            tt += "$t <br>"  // 時刻
            t = st.nextToken()
            tt += " $t "  // 時刻
        }
        else
        {
            // 到着
            tt += "$t 着 "  // 時刻
        }

        // 次の[train]までskip
        while (true)
        {
            if (!st.hasMoreTokens()) break
            t = st.nextToken()
            if (t.contains("[train]") || t.contains("[arr]") )
                break
        }
        if(t.contains("[arr]")) break
        // [train] のみ　次の駅

    }
    if (st.hasMoreTokens()) t = st.nextToken()
    tt += " $t<br>"

    tt = "<!DOCTYPE html>\n" +
            "<html>\n" +
            "<head>\n" +
            "<meta charset=\"utf-8\"></head><body>" +
    "<font size=2 face=\"&#65325;&#65331; &#12468;&#12471;&#12483;&#12463;\">$tt</font></body></html>"
    g.textArea1.text = tt

    val doc = Jsoup.parse(tt)
    val tds = doc.body().html()
    setClipboardHTMLString(tt)
}
