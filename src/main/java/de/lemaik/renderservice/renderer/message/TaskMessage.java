package de.lemaik.renderservice.renderer.message;

public class TaskMessage {
    protected String job_id;
    protected int spp;

    protected TaskMessage() {
        this.job_id = null;
        this.spp = 0;
    }

    public String getJobId() {
        return this.job_id;
    }

    public int getSpp() {
        return this.spp;
    }
}
