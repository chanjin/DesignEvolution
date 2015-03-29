# DesignEvolution


### Steps to run

#### Log 파일 생성
src/main/scala의 소스 처리

##### config/Configurator에서 repository 디렉터리 수정
소스 코드

```scala
  def gitrepo(pname: String) = {
    "/Users/chanjinpark/GitHub/" + pname + "/.git"
  }
```

##### gitlog/GitLogGenerator를 실행
* 현재는 junit으로 되어 있음
* gitlogs 디렉터리에 junit.txt 파일 생성됨

##### gitlog/GitCommitDetails를 실행
* gitlogs/junit 디렉터리 생성해야 함
* gitlogs/junit 디렉터리 아래에 각 commits 별 상세 변경 사항 파일들이 만들어짐 

