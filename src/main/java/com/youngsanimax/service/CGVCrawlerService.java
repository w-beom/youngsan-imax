package com.youngsanimax.service;

import com.youngsanimax.domain.Theater;
import com.youngsanimax.domain.browser.Browser;
import com.youngsanimax.domain.cinema.CGV;
import com.youngsanimax.domain.messenger.Message;
import com.youngsanimax.domain.messenger.TelegramBot;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ScheduledFuture;

@Slf4j
@Service
public class CGVCrawlerService implements CrawlerService {
    private final TelegramBot telegramBot;
    private final TaskScheduler taskScheduler;
    private WebDriver webDriver;

    public CGVCrawlerService(TelegramBot telegramBot, TaskScheduler taskScheduler) {
        this.telegramBot = telegramBot;
        this.taskScheduler = taskScheduler;
    }

    public void crawling(Browser browser) {
        telegramBot.sendMessage(new Message("5267186305", "서비스를 시작합니다."));
        browser.createWebDriver();
        webDriver = browser.openUrl(getUrl());

        List<CGV> days = findDays(webDriver);
        telegramBot.sendMessage(new Message("5267186305", days.toString()));

        scheduleStart(webDriver);
    }

    private void scheduleStart(WebDriver webDriver) {
        ScheduledFuture<?> future = this.taskScheduler.schedule(() -> {
            webDriver.navigate().refresh(); //새로고침
            List<CGV> delayDays = findDays(webDriver);
            telegramBot.sendMessage(new Message("5267186305", "두번 째 " + delayDays.toString()));
        }, new CronTrigger("*/30 * * * * *"));
    }

    private List<CGV> findDays(WebDriver webDriver) {
        List<CGV> movies = new ArrayList<>();
        List<WebElement> days = webDriver.findElements(By.xpath("//*[@id=\"date_list\"]/ul/div/li"));
        for (WebElement day : days) {
            String className = day.getAttribute("class");
            if (className.contains("dimmed")) {
                continue;
            }
            String movieDate = day.getAttribute("date");
            movies.add(new CGV(movieDate, "아바타", "20030160"));
        }
        return movies;
    }

    private String getUrl() {
        String movieCdGroup = "20030160";
        Theater theaterCode = Theater.YONGSAN;
        String today = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));

        return "http://ticket.cgv.co.kr/Reservation/Reservation.aspx?MOVIE_CD=20031534&MOVIE_CD_GROUP=" + movieCdGroup + "&PLAY_YMD=" + today + "&THEATER_CD=" + theaterCode.getCode() + "&PLAY_NUM=&PLAY_START_TM=&AREA_CD=&SCREEN_CD=&THIRD_ITEM=&SCREEN_RATING_CD=";
    }
}
