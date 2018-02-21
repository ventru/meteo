package ru.anosov.meteoapp.synchronize;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Date;

public class SyncDB {

    private String dates;

    public SyncDB(String incomingDates) {
        this.dates = incomingDates;
    }

    private String getDates() {
        return dates;
    }

    public List<String> searchLostDates(){

        String[] parseString;
        //разделитель
        String delimeter = ",";

        //разбить
        parseString = getDates().split(delimeter);

        //получить последнюю дату
        String lastDate = parseString[parseString.length-1];
        System.out.println("Last - " + lastDate);

        //узнать какое сегодня число
        Date dateSet = new Date();
        SimpleDateFormat formatForDateToday = new SimpleDateFormat("yyyy-MM-dd");
        String dateToday = formatForDateToday.format(dateSet);
        System.out.println("Now - " + dateToday);

        //проверка на совпадения
        if (dateToday.equals(lastDate)){
            System.out.println("Даты совпадают, ничего делать не нужно");
            //System.exit(0); //завершить работу программы
        }

        //убрать лишние символы
        dateToday = dateToday.replaceAll("-","");
        lastDate = lastDate.replaceAll("-","");
        System.out.println(dateToday);
        System.out.println(lastDate);

        char[] parseDateToday = dateToday.toCharArray();
        char[] parseDateLast = lastDate.toCharArray();

        String yearToday = String.valueOf(parseDateToday[0]) + String.valueOf(parseDateToday[1]) + String.valueOf(parseDateToday[2]) + String.valueOf(parseDateToday[3]);
        String monthToday = String.valueOf(parseDateToday[4]) + String.valueOf(parseDateToday[5]);
        String dayToday = String.valueOf(parseDateToday[6]) + String.valueOf(parseDateToday[7]);

        String yearLast = String.valueOf(parseDateLast[0]) + String.valueOf(parseDateLast[1]) + String.valueOf(parseDateLast[2]) + String.valueOf(parseDateLast[3]);
        String monthLast = String.valueOf(parseDateLast[4]) + String.valueOf(parseDateLast[5]);
        String dayLast = String.valueOf(parseDateLast[6]) + String.valueOf(parseDateLast[7]);

        System.out.println(yearToday + "|" + monthToday + "|" + dayToday);
        System.out.println(yearLast + "|" + monthLast + "|" + dayLast);

        int yearTodayInt = Integer.valueOf(yearToday);
        int monthTodayInt = Integer.valueOf(monthToday);
        int dayTodayInt = Integer.valueOf(dayToday);

        int yearLastInt = Integer.valueOf(yearLast);
        int monthLastInt = Integer.valueOf(monthLast);
        int dayLastInt = Integer.valueOf(dayLast);

        List<String> lostDates = new ArrayList<>();

        String tmpDay;
        if (yearTodayInt >= yearLastInt){
            for (int i = dayLastInt; i <= 366; i++) {
                dayLastInt++;
                if (monthLastInt < 10){
                    String tmpMonth = "0" + monthLastInt;
                    if (dayLastInt<10) {
                        tmpDay = "0" + dayLastInt;
                    } else {
                        tmpDay = String.valueOf(dayLastInt);
                    }
                    lostDates.add(String.valueOf(yearLastInt + tmpMonth + tmpDay));
                }
                if (monthLastInt>=10){
                    if (dayLastInt<10) {
                        tmpDay = "0" + dayLastInt;
                    } else {
                        tmpDay = String.valueOf(dayLastInt);
                    }
                    lostDates.add(String.valueOf(yearLastInt + monthLastInt + tmpDay));
                }
                if (monthLastInt < 7) {
                    if (dayLastInt == 31) {
                        if (monthLastInt % 2 == 1) {
                            dayLastInt = 0;
                            monthLastInt++;
                        }
                    }
                    if (dayLastInt == 30) {
                        if (monthLastInt % 2 == 0) {
                            dayLastInt = 0;
                            monthLastInt++;
                        }
                    }
                    if ((dayLastInt == 28) && (monthLastInt == 2)) {
                        if (yearLastInt % 4 != 0) {
                            dayLastInt = 0;
                            monthLastInt++;
                        }
                    } else if ((dayLastInt == 29) && (monthLastInt == 2)) {
                        if (yearLastInt % 4 == 0) {
                            if ((yearLastInt % 100 != 0) || (yearLastInt % 400 == 0)) {
                                dayLastInt = 0;
                                monthLastInt++;
                            }
                        }
                    }
                } else if (monthLastInt != 7) {
                    if (dayLastInt == 30) {
                        if (monthLastInt % 2 == 1) {
                            dayLastInt = 0;
                            monthLastInt++;
                        }
                    }
                    if (dayLastInt == 31) {
                        if (monthLastInt % 2 == 0) {
                            dayLastInt = 0;
                            monthLastInt++;
                        }
                    }
                } else {
                    if (dayLastInt == 31) {
                        dayLastInt = 0;
                        monthLastInt++;
                    }
                }
                if (monthLastInt > 12){
                    monthLastInt = 1;
                    dayLastInt=0;
                    yearLastInt++;
                }
                if ((monthLastInt == monthTodayInt) && (dayLastInt == dayTodayInt) && (yearLastInt == yearTodayInt)){
                    break;
                }
            }
        }
        for (int i = 0; i < lostDates.size(); i++) {
            System.out.println(lostDates.get(i));
        }
        return lostDates;
    }

}
