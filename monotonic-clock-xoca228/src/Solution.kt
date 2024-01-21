/**
 * В теле класса решения разрешено использовать только переменные делегированные в класс RegularInt.
 * Нельзя volatile, нельзя другие типы, нельзя блокировки, нельзя лазить в глобальные переменные.
 *
 * @author TOLEPBEK TEMIRLAN
 */
class Solution : MonotonicClock {
    private var c1 by RegularInt(0)
    private var c2 by RegularInt(0)
    private var c3 by RegularInt(0)
    private var v1 by RegularInt(0)
    private var v2 by RegularInt(0)

    override fun write(time: Time) {
        v1 = time.d1
        v2 = time.d2

        c3 = time.d3
        c2 = time.d2
        c1 = time.d1
    }

    override fun read(): Time {
        val r11 = c1
        val r12 = c2
        val r13 = c3

        val r22 = v2
        val r21 = v1

        return if (r11 == r21) {
            if (r12 == r22) {
                Time(r11, r12, r13)
            } else {
                Time(r11, r22, 0)
            }
        } else {
            Time(r21, 0, 0)
        }
    }
}