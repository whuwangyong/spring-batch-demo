package demo;


import org.springframework.batch.core.*;
import org.springframework.batch.core.configuration.JobRegistry;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.FlatFileItemWriter;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.core.io.FileSystemResource;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * demo.Demo for spring batch
 * Created by wuzhaofeng on 17/7/23.
 */

public class Main {


    public static void main(String[] args) throws Exception {

        // 加载上下文
        String[] configLocations = {"applicationContext.xml"};
        ApplicationContext applicationContext = new ClassPathXmlApplicationContext(configLocations);

        // 获取任务启动器
        JobLauncher jobLauncher = applicationContext.getBean(JobLauncher.class);
        JobRepository jobRepository = applicationContext.getBean(JobRepository.class);

        PlatformTransactionManager transactionManager = applicationContext.getBean(PlatformTransactionManager.class);

        // 创建reader
        FlatFileItemReader<DeviceCommand> flatFileItemReader = new FlatFileItemReader<>();
        flatFileItemReader.setResource(new FileSystemResource("src/main/resources/batch-data-source.csv"));
        flatFileItemReader.setLineMapper(new HelloLineMapper());

        // 创建processor
        HelloItemProcessor helloItemProcessor = new HelloItemProcessor();

        // 创建writer
        FlatFileItemWriter<DeviceCommand> flatFileItemWriter = new FlatFileItemWriter<>();
        flatFileItemWriter.setResource(new FileSystemResource("src/main/resources/batch-data-target.csv"));
        flatFileItemWriter.setLineAggregator(new HelloLineAggregator());


        // 创建Step
        StepBuilderFactory stepBuilderFactory = new StepBuilderFactory(jobRepository, transactionManager);
        Step step = stepBuilderFactory.get("step")
                          .<DeviceCommand, DeviceCommand>chunk(5)
                          .reader(flatFileItemReader)       // 读操作
                          .processor(helloItemProcessor)    // 处理操作
                          .writer(flatFileItemWriter)       // 写操作
                  //        .allowStartIfComplete(true)
                          .build();

        // 创建Job
        JobBuilderFactory jobBuilderFactory = new JobBuilderFactory(jobRepository);
        Job job = jobBuilderFactory.get("job4")
                                   .start(step)
                                   .build();




        // 启动任务
        Map<String, JobParameter> map = new HashMap<>();
        map.put("test",new JobParameter("test"));
        JobParameters parameters = new JobParameters(map);

        JobExecution jobExecution = jobRepository.getLastJobExecution("job4",parameters);
        jobExecution.setStatus(BatchStatus.FAILED);
        jobExecution.setEndTime(jobExecution.getStartTime());
        jobExecution.setExitStatus(ExitStatus.FAILED);
        jobRepository.update(jobExecution);

        jobLauncher.run(job, parameters);

    }

    public DataSource dataSource() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setDriverClassName("com.mysql.cj.jdbc.Driver");
        dataSource.setUrl("jdbc:mysql://localhost:3306/test?useUnicode=false&characterEncoding=utf8&serverTimezone=Asia/Shanghai&useSSL=false");
        dataSource.setUsername("admin");
        dataSource.setPassword("admin");
        return dataSource;
    }

}
