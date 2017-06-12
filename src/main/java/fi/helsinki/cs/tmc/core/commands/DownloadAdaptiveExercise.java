package fi.helsinki.cs.tmc.core.commands;

import fi.helsinki.cs.tmc.core.communication.TmcServerCommunicationTaskFactory;
import fi.helsinki.cs.tmc.core.domain.Exercise;
import fi.helsinki.cs.tmc.core.domain.Progress;
import fi.helsinki.cs.tmc.core.domain.ProgressObserver;
import fi.helsinki.cs.tmc.core.holders.TmcSettingsHolder;

import com.google.common.annotations.VisibleForTesting;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DownloadAdaptiveExercise extends ExerciseDownloadingCommand<Exercise> {

    private static final Logger logger = LoggerFactory.getLogger(DownloadAdaptiveExercise.class);

    public DownloadAdaptiveExercise(ProgressObserver observer) {
        super(observer);
    }

    @VisibleForTesting
    DownloadAdaptiveExercise(ProgressObserver observer, TmcServerCommunicationTaskFactory tmcServerCommunicationTaskFactory) {
        super(observer, tmcServerCommunicationTaskFactory);
    }

    @Override
    public Exercise call() throws Exception {
        logger.info("Checking adaptive exercises availability");
        Exercise exercise = tmcServerCommunicationTaskFactory.getAdaptiveExercise().call();
        if (exercise == null) {
            return null;
        }
        try {
            exercise.setCourseName(TmcSettingsHolder.get().getCurrentCourse().get().getName());
        } catch (Exception e) {
            logger.warn("Setting course name failed, setting it to 'None'.", e);
            exercise.setCourseName("None");
        }
        exercise.setReturnable(true);
        exercise.setAdaptive(true);
        byte[] zipb = tmcServerCommunicationTaskFactory.getDownloadingExerciseZipTask(exercise).call();
        // Progress gets 1 as a parameter, because there is only 1 exercise to extract.
        Progress progress = new Progress(1);
        extractProject(zipb, exercise, progress);
        return exercise;
    }
}
