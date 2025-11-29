package prontuario.al.test.annotations

import org.springframework.boot.test.autoconfigure.graphql.tester.AutoConfigureHttpGraphQlTester
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ContextConfiguration
import prontuario.al.Application

@Target(AnnotationTarget.CLASS)
@SpringBootTest
@AutoConfigureHttpGraphQlTester
@ContextConfiguration(classes = [Application::class])
annotation class HttpLayerTest
